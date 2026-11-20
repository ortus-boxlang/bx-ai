//! Native backing map for BoxAiService. One instance per declaring module/VM.
//! Only capability tables are process-global. No VM value or mutable registry is.
mod atomic_integer;
use matchbox_plugin_sdk::*;
use std::{cell::RefCell, collections::HashMap, ffi::c_void, sync::OnceLock};

const MAP: &str = "bxai.serverMap.v1";
struct Api {
    roots: &'static RootsV1,
    collections: &'static CollectionsV1,
    types: &'static TypesV1,
    integers: &'static IntegersV1,
}
static API: OnceLock<Api> = OnceLock::new();
struct Entry {
    spelling: String,
    instance: RetainedValue,
}
type ServerMap = RefCell<HashMap<String, Entry>>;

unsafe extern "C" fn drop_map(data: *mut c_void) {
    drop(unsafe { Box::from_raw(data.cast::<ServerMap>()) });
}

unsafe extern "C" fn factory(
    ctx: Context,
    api: *const ValuesV1,
    _: *mut c_void,
    args: *const Value,
    len: usize,
    out: *mut Value,
) -> i32 {
    unsafe {
        guarded(ctx, api, out, || {
            let call = Call::new(ctx, api, args, len);
            let data = Box::into_raw(Box::<ServerMap>::default()).cast();
            let result = call.object_new(MAP, data, drop_map, r#"{"size":"bxAiMapSize","isEmpty":"bxAiMapIsEmpty","names":"bxAiMapNames","clear":"bxAiMapClear","get":"bxAiMapGet","containsKey":"bxAiMapContains","put":"bxAiMapPut","putIfAbsent":"bxAiMapPutIfAbsent","getOrInsert":"bxAiMapGetOrInsert","remove":"bxAiMapRemove"}"#);
            if result.is_err() {
                drop_map(data);
            }
            result
        })
    }
}

#[derive(Clone, Copy)]
struct Operation {
    name: &'static str,
    params: &'static [&'static str],
    callback: Invoke,
}
macro_rules! operations {
    ($($variant:ident => ($name:literal, [$($param:literal),*])),* $(,)?) => {
        #[repr(u8)] enum Op { $($variant),* }
        const OPERATIONS: &[Operation] = &[$(Operation {name: $name, params: &["receiver", $($param),*], callback: method::<{Op::$variant as u8}>}),*];
    };
}
operations! {
    Size => ("bxAiMapSize", []), IsEmpty => ("bxAiMapIsEmpty", []), Names => ("bxAiMapNames", []), Clear => ("bxAiMapClear", []),
    Get => ("bxAiMapGet", ["name"]), Contains => ("bxAiMapContains", ["name"]), Remove => ("bxAiMapRemove", ["name"]),
    Put => ("bxAiMapPut", ["name","serverInstance"]), PutIfAbsent => ("bxAiMapPutIfAbsent", ["name","serverInstance"]), GetOrInsert => ("bxAiMapGetOrInsert", ["name","serverInstance"]),
}

unsafe extern "C" fn method<const OP: u8>(
    ctx: Context,
    values: *const ValuesV1,
    _: *mut c_void,
    args: *const Value,
    len: usize,
    out: *mut Value,
) -> i32 {
    unsafe {
        guarded(ctx, values, out, || {
            let call = Call::new(ctx, values, args, len);
            let api = API.get().ok_or("bx-ai capabilities unavailable")?;
            let ptr = call
                .object_data(call.argument(0)?, MAP)?
                .cast::<ServerMap>();
            // Payload is type-checked by the host, VM-thread-affine and live for this call.
            // No BoxLang callbacks or suspension inside a map operation.
            let mut map = ptr
                .as_ref()
                .try_borrow_mut()
                .map_err(|_| "Reentrant server map access")?;
            if OP == Op::Size as u8 {
                return call.int_new(api.integers, map.len() as i64);
            }
            if OP == Op::IsEmpty as u8 {
                return call.bool_new(map.is_empty());
            }
            if OP == Op::Clear as u8 {
                map.clear();
                return call.null_new();
            }
            if OP == Op::Names as u8 {
                let result = call.array_new(api.collections)?;
                for entry in map.values() {
                    call.array_push(api.collections, result, call.string_new(&entry.spelling)?)?;
                }
                return Ok(result);
            }
            let spelling = call.string(call.argument(1)?)?;
            let key = spelling.to_lowercase();
            if OP == Op::Contains as u8 {
                return call.bool_new(map.contains_key(&key));
            }
            let previous = map.get(&key);
            let previous_value = || match previous {
                Some(entry) => entry.instance.value(&call),
                None => call.null_new(),
            };
            if OP == Op::Get as u8 {
                return previous_value();
            }
            if OP == Op::Remove as u8 {
                let result = previous_value()?;
                map.remove(&key);
                return Ok(result);
            }
            let supplied = call.argument(2)?;
            if call.kind(api.types, supplied)? != Kind::Other {
                return Err("Expected a BoxLang server instance".into());
            }
            if OP != Op::Put as u8 && previous.is_some() {
                return previous_value();
            }
            let result = if OP == Op::GetOrInsert as u8 {
                supplied
            } else {
                previous_value()?
            };
            let spelling = previous.map_or(spelling, |entry| entry.spelling.clone());
            map.insert(
                key,
                Entry {
                    spelling,
                    instance: call.retain(api.roots, supplied)?,
                },
            );
            Ok(result)
        })
    }
}

/// # Safety
/// Called by the ABI-1 host with valid registration context and capability tables.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn matchbox_plugin_init_v1(host: *const HostV1, ctx: Context) -> i32 {
    unsafe {
        macro_rules! require {
            ($f:ident) => {
                match $f(host, ctx) {
                    Ok(v) => v,
                    Err(status) => return status,
                }
            };
        }
        require!(require_values);
        let _ = API.set(Api {
            roots: require!(require_roots),
            types: require!(require_types),
            collections: require!(require_collections),
            integers: require!(require_integers),
        });
        let factory = Operation {
            name: "bxAiNativeServerMap",
            params: &[],
            callback: factory,
        };
        for op in std::iter::once(&factory)
            .chain(OPERATIONS)
            .chain(atomic_integer::OPERATIONS)
        {
            let params = op
                .params
                .iter()
                .map(|p| format!(r#"{{"name":"{p}","required":true}}"#))
                .collect::<Vec<_>>()
                .join(",");
            let descriptor = format!(r#"{{"name":"{}","arguments":[{}]}}"#, op.name, params);
            let status = ((*host).register)(ctx, Text::new(&descriptor), op.callback);
            if status != 0 {
                return status;
            }
        }
        0
    }
}

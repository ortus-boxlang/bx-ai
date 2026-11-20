//! The stats counter matches the JVM AtomicInteger's width, wraparound and ordering.
use super::{API, Operation};
use matchbox_plugin_sdk::*;
use std::{
    ffi::c_void,
    sync::atomic::{AtomicI32, Ordering},
};

const COUNTER: &str = "bxai.atomicInteger.v1";
pub(super) const OPERATIONS: &[Operation] = &[
    Operation {
        name: "bxAiNativeAtomicInteger",
        params: &["initial"],
        callback: create,
    },
    Operation {
        name: "bxAiCounterGet",
        params: &["receiver"],
        callback: read_or_add::<0>,
    },
    Operation {
        name: "bxAiCounterIncrement",
        params: &["receiver"],
        callback: read_or_add::<1>,
    },
    Operation {
        name: "bxAiCounterDecrement",
        params: &["receiver"],
        callback: read_or_add::<-1>,
    },
];

unsafe extern "C" fn drop_counter(data: *mut c_void) {
    drop(unsafe { Box::from_raw(data.cast::<AtomicI32>()) });
}

unsafe extern "C" fn create(
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
            let integers = API.get().ok_or("bx-ai capabilities unavailable")?.integers;
            let initial = i32::try_from(call.integer(integers, call.argument(0)?)?)
                .map_err(|_| "Expected a signed 32-bit integer")?;
            let data = Box::into_raw(Box::new(AtomicI32::new(initial))).cast();
            let result = call.object_new(COUNTER, data, drop_counter, r#"{"get":"bxAiCounterGet","incrementAndGet":"bxAiCounterIncrement","decrementAndGet":"bxAiCounterDecrement"}"#);
            if result.is_err() {
                drop_counter(data);
            }
            result
        })
    }
}

unsafe extern "C" fn read_or_add<const DELTA: i32>(
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
            let counter = call
                .object_data(call.argument(0)?, COUNTER)?
                .cast::<AtomicI32>();
            let counter = counter.as_ref();
            let result = if DELTA == 0 {
                counter.load(Ordering::SeqCst)
            } else {
                counter
                    .fetch_add(DELTA, Ordering::SeqCst)
                    .wrapping_add(DELTA)
            };
            call.int_new(
                API.get().ok_or("bx-ai capabilities unavailable")?.integers,
                i64::from(result),
            )
        })
    }
}

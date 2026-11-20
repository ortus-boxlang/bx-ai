//! Native provider for the module-owned Java static factory. Reference contents are
//! VM-traced edges, not plugin roots, so a cell may even contain itself.
use super::{API, atomic_integer};
use matchbox_plugin_sdk::*;
use std::ffi::c_void;

const CLASS: &str = "bxai.AtomicValues.v1";
const REFERENCE: &str = "bxai.AtomicReference.v1";

pub(super) unsafe fn register(host: *const HostV1, ctx: Context) -> Result<(), i32> {
    let providers = unsafe { require_class_providers(host, ctx)? };
    for (metadata, invoke) in [
        (
            r#"{"name":"bxAiAtomicValuesInteger","arguments":[{"name":"receiver","required":true},{"name":"initial","required":true}]}"#,
            integer as Invoke,
        ),
        (
            r#"{"name":"bxAiAtomicValuesReference","arguments":[{"name":"receiver","required":true},{"name":"initial","required":true,"nullable":true}]}"#,
            reference as Invoke,
        ),
        (
            r#"{"name":"bxAiReferenceGet","arguments":[{"name":"receiver","required":true}]}"#,
            get as Invoke,
        ),
        (
            r#"{"name":"bxAiReferenceSet","arguments":[{"name":"receiver","required":true},{"name":"value","required":true,"nullable":true}]}"#,
            set as Invoke,
        ),
    ] {
        let status = unsafe { ((*host).register)(ctx, Text::new(metadata), invoke) };
        if status != 0 {
            return Err(status);
        }
    }
    let status = unsafe {
        (providers.declare)(
            ctx,
            Text::new("java:ortus.boxlang.ai.util.AtomicValues"),
            class_reference,
        )
    };
    if status == 0 { Ok(()) } else { Err(status) }
}

// Keep call handles within guarded's invocation; only the host stores the slot values.
macro_rules! method {
    ($name:ident, |$call:ident, $api:ident| $body:block) => {
        unsafe extern "C" fn $name(
            ctx: Context,
            values: *const ValuesV1,
            _: *mut c_void,
            args: *const Value,
            len: usize,
            out: *mut Value,
        ) -> i32 {
            unsafe {
                guarded(ctx, values, out, || {
                    let $call = Call::new(ctx, values, args, len);
                    let $api = API.get().ok_or("bx-ai capabilities unavailable")?;
                    $body
                })
            }
        }
    };
}
method!(class_reference, |call, api| {
    call.state_object(
        api.state,
        CLASS,
        r#"{"integer":"bxAiAtomicValuesInteger","reference":"bxAiAtomicValuesReference"}"#,
    )
});
method!(integer, |call, api| {
    call.object_state(api.state, call.argument(0)?, CLASS)?;
    atomic_integer::new_counter(&call, call.argument(1)?)
});
method!(reference, |call, api| {
    call.object_state(api.state, call.argument(0)?, CLASS)?;
    let object = call.state_object(
        api.state,
        REFERENCE,
        r#"{"get":"bxAiReferenceGet","set":"bxAiReferenceSet"}"#,
    )?;
    let slots = call.object_state(api.state, object, REFERENCE)?;
    call.struct_set(slots, "value", call.argument(1)?)?;
    Ok(object)
});
method!(get, |call, api| {
    let slots = call.object_state(api.state, call.argument(0)?, REFERENCE)?;
    match call.struct_get(api.collections, slots, "value")? {
        Some(value) => Ok(value),
        None => call.null_new(),
    }
});
method!(set, |call, api| {
    let slots = call.object_state(api.state, call.argument(0)?, REFERENCE)?;
    call.struct_set(slots, "value", call.argument(1)?)?;
    call.null_new()
});

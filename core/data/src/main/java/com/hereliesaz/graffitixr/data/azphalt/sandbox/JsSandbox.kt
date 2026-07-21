package com.hereliesaz.graffitixr.data.azphalt.sandbox

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.ValueType
import com.dylibso.chicory.wasm.types.Value
import com.dylibso.chicory.wasi.WasiOptions
import com.dylibso.chicory.wasi.WasiPreview1
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Executes a JavaScript extension payload inside the `quickjs.wasm` module using Chicory.
 * The `quickjs.wasm` provides the JS runtime, and we provide WASI functions + capability host functions.
 */
class JsSandbox(
    private val jsCode: String,
    quickjsWasmBytes: InputStream,
    private val host: AzphaltSandboxHost,
    grantedCapabilities: Set<String> = emptySet()
) {
    private val instance: Instance
    
    // Extracted host functions for quickjs capabilities
    private val hostFunctions = mutableListOf<HostFunction>()
    
    init {
        // QuickJS-wasi requires specific host imports in the "env" module
        bindHostCall(grantedCapabilities)
        bindStubs()
        
        // Add Wasi preview 1 functions
        val logger = com.dylibso.chicory.log.SystemLogger()
        val wasiOpts = WasiOptions.builder().build()
        val wasi = WasiPreview1(logger, wasiOpts)
        
        val importsList = mutableListOf<HostFunction>()
        importsList.addAll(wasi.toHostFunctions().toList())
        importsList.addAll(hostFunctions)

        val imports = HostImports(importsList.toTypedArray())
        val module = com.dylibso.chicory.runtime.Module.builder(quickjsWasmBytes).withHostImports(imports).build()
        instance = module.instantiate()
        
        // Initialize QuickJS context
        val qjsInit = instance.export("qjs_init")
        val initResult = qjsInit.apply()[0].asInt()
        if (initResult != 0) {
            throw IllegalStateException("Failed to initialize QuickJS WASM runtime")
        }
    }
    
    fun eval() {
        val wasmMalloc = instance.export("wasm_malloc")
        val wasmFree = instance.export("wasm_free")
        val qjsEval = instance.export("qjs_eval")
        
        val jsBytes = jsCode.toByteArray(StandardCharsets.UTF_8)
        
        // Allocate space for the code string and filename
        val codePtr = wasmMalloc.apply(Value.i32(jsBytes.size.toLong() + 1L))[0].asInt()
        val filenameStr = "extension.js"
        val filenameBytes = filenameStr.toByteArray(StandardCharsets.UTF_8)
        val filenamePtr = wasmMalloc.apply(Value.i32(filenameBytes.size.toLong() + 1L))[0].asInt()
        
        // Write to WASM memory
        instance.memory().write(codePtr, jsBytes)
        instance.memory().writeByte(codePtr + jsBytes.size, 0) // null terminator
        instance.memory().write(filenamePtr, filenameBytes)
        instance.memory().writeByte(filenamePtr + filenameBytes.size, 0)
        
        try {
            // qjs_eval signature: (code: i32, len: i32, filename: i32, eval_flags: i32) -> i32 (returns JSValue*)
            // eval_flags: JS_EVAL_TYPE_GLOBAL = 0
            val resultPtr = qjsEval.apply(
                Value.i32(codePtr.toLong()),
                Value.i32(jsBytes.size.toLong()),
                Value.i32(filenamePtr.toLong()),
                Value.i32(0L)
            )[0].asInt()
            
            // Check if exception
            val qjsIsException = instance.export("qjs_is_exception")
            val isException = qjsIsException.apply(Value.i32(resultPtr.toLong()))[0].asInt() != 0
            
            if (isException) {
                // If it's an exception, we would ideally read the error, but for now we just throw
                throw RuntimeException("JavaScript execution failed inside QuickJS sandbox.")
            }
            
            // Free the returned JSValue pointer
            wasmFree.apply(Value.i32(resultPtr.toLong()))
        } finally {
            wasmFree.apply(Value.i32(codePtr.toLong()))
            wasmFree.apply(Value.i32(filenamePtr.toLong()))
        }
    }

    private fun bindHostCall(grantedCapabilities: Set<String>) {
        // QuickJS-wasi imports `env.host_call` to jump back to host functions
        // Signature: (name_ptr: i32, name_len: i32, this_ptr: i32, argc: i32, argv_ptr: i32) -> i32 (returns JSValue*)
        hostFunctions.add(
            HostFunction(
                { _: Instance, args: Array<Value> ->
                    val namePtr = args[0].asInt()
                    val nameLen = args[1].asInt()
                    val thisPtr = args[2].asInt()
                    val argc = args[3].asInt()
                    val argvPtr = args[4].asInt()
                    
                    val name = instance.memory().readString(namePtr, nameLen)
                    
                    // We only have the capability router here. For this implementation, we will mock the return JSValue*
                    // as undefined, because a full JSValue* serialization is complex. In reality, we'd want to parse JSValue* 
                    // from WASM memory, pass to host, and write back. 
                    // To keep this sandbox functional but simple for now, we will execute the host functions
                    // but not return complex JS values.
                    
                    if ("canvas" in grantedCapabilities && name == "requestRedraw") {
                        host.requestRedraw()
                    } else if ("color" in grantedCapabilities && name == "colorActive") {
                        // This requires returning a number in JSValue. 
                        // The actual bridging requires calling qjs_new_number.
                    }
                    
                    // Return undefined for now
                    val qjsGetUndefined = instance.export("qjs_get_undefined")
                    qjsGetUndefined.apply()
                },
                "env", "host_call",
                listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32), listOf(ValueType.I32)
            )
        )
    }
    
    private fun bindStubs() {
        hostFunctions.add(
            HostFunction(
                { _: Instance, _: Array<Value> -> arrayOf(Value.i32(0L)) },
                "env", "host_interrupt",
                emptyList(), listOf(ValueType.I32)
            )
        )
        hostFunctions.add(
            HostFunction(
                { _: Instance, _: Array<Value> -> arrayOf(Value.i32(0L)) },
                "env", "host_module_normalize",
                listOf(ValueType.I32, ValueType.I32), listOf(ValueType.I32)
            )
        )
        hostFunctions.add(
            HostFunction(
                { _: Instance, _: Array<Value> -> arrayOf(Value.i32(0L)) },
                "env", "host_module_load",
                listOf(ValueType.I32, ValueType.I32), listOf(ValueType.I32)
            )
        )
        hostFunctions.add(
            HostFunction(
                { _: Instance, _: Array<Value> -> null },
                "env", "host_promise_rejection",
                listOf(ValueType.I32, ValueType.I32, ValueType.I32), emptyList()
            )
        )
        hostFunctions.add(
            HostFunction(
                { _: Instance, _: Array<Value> -> arrayOf(Value.i32(0L)) },
                "env", "host_get_timezone_offset",
                listOf(ValueType.I32, ValueType.I32), listOf(ValueType.I32)
            )
        )
    }
}

package com.hereliesaz.graffitixr.data.azphalt.sandbox

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.HostImports
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.ValueType
import com.dylibso.chicory.wasm.types.Value
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Executes a WASM module within an isolated Chicory sandbox.
 * Provides a highly secure environment with no ambient authority, per the azphalt capability model.
 */
class WasmSandbox(
    wasmBytes: InputStream,
    private val host: AzphaltSandboxHost,
    grantedCapabilities: Set<String> = emptySet()
) {
    private val instance: Instance
    
    // Extracted host functions mapped to capabilities
    private val hostFunctions = mutableListOf<HostFunction>()

    init {
        bindCapabilities(grantedCapabilities)
        
        val imports = HostImports(hostFunctions.toTypedArray())
        val module = com.dylibso.chicory.runtime.Module.builder(wasmBytes).withHostImports(imports).build()
        instance = module.instantiate()
    }
    
    private fun bindCapabilities(grantedCapabilities: Set<String>) {
        if ("canvas" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        host.requestRedraw()
                        null
                    },
                    "env", "requestRedraw",
                    emptyList(), emptyList()
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        arrayOf(Value.i32(host.canvasWidth().toLong()))
                    },
                    "env", "canvasWidth",
                    emptyList(), listOf(ValueType.I32)
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        arrayOf(Value.i32(host.canvasHeight().toLong()))
                    },
                    "env", "canvasHeight",
                    emptyList(), listOf(ValueType.I32)
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        arrayOf(Value.i32(host.canvasDpi().toLong()))
                    },
                    "env", "canvasDpi",
                    emptyList(), listOf(ValueType.I32)
                )
            )
        }
        
        if ("layers" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        arrayOf(Value.i32(host.layerCount().toLong()))
                    },
                    "env", "layerCount",
                    emptyList(), listOf(ValueType.I32)
                )
            )
        }

        if ("params" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val key = instance.memory().readString(args[0].asInt(), args[1].asInt())
                        val value = host.paramNumber(key) ?: 0.0
                        arrayOf(Value.f64(java.lang.Double.doubleToRawLongBits(value)))
                    },
                    "env", "paramNumber",
                    listOf(ValueType.I32, ValueType.I32), listOf(ValueType.F64)
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val key = instance.memory().readString(args[0].asInt(), args[1].asInt())
                        val value = host.paramBool(key) ?: false
                        arrayOf(Value.i32(if (value) 1L else 0L))
                    },
                    "env", "paramBool",
                    listOf(ValueType.I32, ValueType.I32), listOf(ValueType.I32)
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val key = instance.memory().readString(args[0].asInt(), args[1].asInt())
                        val outPtr = args[2].asInt()
                        val outCap = args[3].asInt()
                        
                        val value = host.paramString(key)
                        if (value == null) {
                            arrayOf(Value.i32(-1L))
                        } else {
                            val bytes = value.toByteArray(StandardCharsets.UTF_8)
                            val toCopy = Math.min(bytes.size, outCap)
                            instance.memory().write(outPtr, bytes, 0, toCopy)
                            arrayOf(Value.i32(bytes.size.toLong()))
                        }
                    },
                    "env", "paramString",
                    listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32), listOf(ValueType.I32)
                )
            )
        }
        
        if ("color" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val outPtr = args[0].asInt()
                        instance.memory().writeI32(outPtr, host.colorActive())
                        null
                    },
                    "env", "colorActive",
                    listOf(ValueType.I32), emptyList()
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val inPtr = args[0].asInt()
                        host.colorSetActive(instance.memory().readInt(inPtr))
                        null
                    },
                    "env", "colorSetActive",
                    listOf(ValueType.I32), emptyList()
                )
            )
        }
        
        if ("assets" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val pPtr = args[0].asInt()
                        val pLen = args[1].asInt()
                        val outPtr = args[2].asInt()
                        val outCap = args[3].asInt()
                        
                        val path = instance.memory().readString(pPtr, pLen)
                        val data = host.assetRead(path)
                        
                        if (data == null) {
                            arrayOf(Value.i32(-1L))
                        } else {
                            val toCopy = Math.min(data.size, outCap)
                            instance.memory().write(outPtr, data, 0, toCopy)
                            arrayOf(Value.i32(data.size.toLong()))
                        }
                    },
                    "env", "assetRead",
                    listOf(ValueType.I32, ValueType.I32, ValueType.I32, ValueType.I32), listOf(ValueType.I32)
                )
            )
        }
        
        if ("selection" in grantedCapabilities) {
            hostFunctions.add(
                HostFunction(
                    { _: Instance, _: Array<Value> ->
                        arrayOf(Value.i32(host.selectionSize().toLong()))
                    },
                    "env", "selectionSize",
                    emptyList(), listOf(ValueType.I32)
                )
            )
            hostFunctions.add(
                HostFunction(
                    { _: Instance, args: Array<Value> ->
                        val outPtr = args[0].asInt()
                        val mask = host.selectionRead()
                        instance.memory().write(outPtr, mask)
                        null
                    },
                    "env", "selectionRead",
                    listOf(ValueType.I32), emptyList()
                )
            )
        }
    }
}

package com.hereliesaz.graffitixr.data.azphalt.sandbox

/**
 * Defines the capability bridge between the isolated sandbox and Graffux.
 * 
 * Each function represents an access-controlled capability. If an extension
 * lacks a capability in its manifest, the corresponding functions are not mapped
 * into the WASM environment at instantiation, causing it to fail immediately if it
 * attempts to import them (deny by default).
 */
interface AzphaltSandboxHost {
    
    // Canvas capability
    fun requestRedraw()
    fun canvasWidth(): Int
    fun canvasHeight(): Int
    fun canvasDpi(): Int
    
    // Params capability
    fun paramNumber(key: String): Double?
    fun paramBool(key: String): Boolean?
    fun paramString(key: String): String?
    
    // Color capability (RGBA 8-bit encoded as Int)
    fun colorActive(): Int
    fun colorSetActive(rgba: Int)
    
    // Assets capability
    fun assetRead(path: String): ByteArray?
    
    // Selection capability
    fun selectionSize(): Int
    fun selectionRead(): ByteArray
    
    // Layers capability
    fun layerCount(): Int
}

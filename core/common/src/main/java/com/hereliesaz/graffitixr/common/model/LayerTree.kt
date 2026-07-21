package com.hereliesaz.graffitixr.common.model

data class LayerNode(
    val layer: Layer,
    val depth: Int,
    val children: List<LayerNode>
)

/**
 * Builds a hierarchical tree from a flat list of layers.
 */
fun buildLayerTree(layers: List<Layer>): List<LayerNode> {
    val byParent = layers.groupBy { it.parentId }
    fun build(parentId: String?, depth: Int): List<LayerNode> {
        return (byParent[parentId] ?: emptyList()).map { layer ->
            LayerNode(layer, depth, build(layer.id, depth + 1))
        }
    }
    return build(null, 0)
}

/**
 * Flattens a layer tree back into a list, usually for UI display where nested
 * items need to be shown in a flat scrolling list.
 */
fun flattenTree(nodes: List<LayerNode>): List<LayerNode> {
    val result = mutableListOf<LayerNode>()
    for (node in nodes.reversed()) {
        result.add(node)
        result.addAll(flattenTree(node.children))
    }
    return result
}

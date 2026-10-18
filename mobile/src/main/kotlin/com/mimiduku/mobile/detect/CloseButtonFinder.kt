package com.mimiduku.mobile.detect

import com.mimiduku.mobile.rules.ScarewareMatcher

/** 代理タップの候補を選ぶための、ノードの写し（Android 非依存）。 */
data class NodeSnapshot(
    val text: String?,
    val contentDescription: String?,
    val clickable: Boolean,
    val visible: Boolean,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * 広告画面の本物の閉じるボタン（またはスキップボタン）を選ぶ。
 *
 * 広告本体を誤って押すと広告のクリックになるため（PLAN.md 7章の非目標）、条件を厳しくする。
 * - 文字か説明が閉じる・スキップの文言に**完全一致**する（「今すぐインストールして閉じる」のような文は選ばない）
 * - クリックでき、表示されていて、画面内に収まり、大きさが0でない
 * - 画面の [MAX_AREA_RATIO] を超える大きさではない（大きな領域は広告本体の可能性がある）
 *
 * 閉じるの文言をスキップより優先する。代理タップは利用者が「もどる」を押したときだけ行い、自動では行わない。
 */
object CloseButtonFinder {
    const val MAX_AREA_RATIO = 0.25

    private val closeLabels = setOf("閉じる", "とじる", "×", "✕", "✖", "x", "close", "closead", "広告を閉じる")
    private val skipLabels = setOf("スキップ", "広告をスキップ", "skip", "skipad", "skipvideo")

    /** 選んだノードの位置（[nodes] の添字）。候補が無ければ null。 */
    fun find(
        nodes: List<NodeSnapshot>,
        screenWidth: Int,
        screenHeight: Int,
    ): Int? {
        val eligible = nodes.indices.filter { isEligible(nodes[it], screenWidth, screenHeight) }
        return eligible.firstOrNull { matchesAny(nodes[it], closeLabels) }
            ?: eligible.firstOrNull { matchesAny(nodes[it], skipLabels) }
    }

    private fun isEligible(
        node: NodeSnapshot,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean {
        val inScreen = node.left >= 0 && node.top >= 0 && node.right <= screenWidth && node.bottom <= screenHeight
        val area = node.width.toLong() * node.height
        val screenArea = screenWidth.toLong() * screenHeight
        return node.clickable &&
            node.visible &&
            inScreen &&
            node.width > 0 &&
            node.height > 0 &&
            area <= screenArea * MAX_AREA_RATIO
    }

    private fun matchesAny(
        node: NodeSnapshot,
        labels: Set<String>,
    ): Boolean =
        listOfNotNull(node.text, node.contentDescription)
            .map(ScarewareMatcher::normalize)
            .any { it in labels }
}

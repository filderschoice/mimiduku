package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * ノードツリーから文字（text と contentDescription）を集める。
 *
 * 巨大な画面で処理が重くならないよう、集めるノード数と深さに上限を設ける。集めた文字は照合にだけ使い、
 * 保存・送信しない（MVP_DESIGN.md 10章）。
 */
object NodeTextCollector {
    const val MAX_NODES = 400
    const val MAX_DEPTH = 40

    fun collect(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val texts = mutableListOf<String>()
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = stack.removeLast()
            visited++
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let(texts::add)
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(texts::add)
            if (depth < MAX_DEPTH) pushChildren(stack, node, depth + 1)
        }
        return texts
    }

    /** 画面の上から順に読めるよう、子を逆順に積む。 */
    private fun pushChildren(
        stack: ArrayDeque<Pair<AccessibilityNodeInfo, Int>>,
        node: AccessibilityNodeInfo,
        depth: Int,
    ) {
        for (i in node.childCount - 1 downTo 0) {
            node.getChild(i)?.let { stack.addLast(it to depth) }
        }
    }
}

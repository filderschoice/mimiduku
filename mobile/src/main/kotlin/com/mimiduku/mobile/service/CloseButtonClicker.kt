package com.mimiduku.mobile.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.mimiduku.mobile.detect.CloseButtonFinder
import com.mimiduku.mobile.detect.NodeSnapshot

/**
 * 広告画面の本物の閉じるボタンへ ACTION_CLICK を送る。候補の選び方は [CloseButtonFinder]（Android 非依存）にある。
 *
 * 利用者が案内パネルの「もどる」を押したときだけ呼ぶ。自動ではクリックしない（PLAN.md 7章）。
 */
object CloseButtonClicker {
    /** クリックできたら true。候補が無い・クリックに失敗した場合は false（呼び出し側が「戻る」で代える）。 */
    fun clickClose(
        root: AccessibilityNodeInfo?,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean {
        val nodes = collectNodes(root)
        val snapshots = nodes.map(::snapshot)
        val index = CloseButtonFinder.find(snapshots, screenWidth, screenHeight) ?: return false
        return nodes[index].performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun snapshot(node: AccessibilityNodeInfo): NodeSnapshot {
        val bounds = Rect().also(node::getBoundsInScreen)
        return NodeSnapshot(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            clickable = node.isClickable,
            visible = node.isVisibleToUser,
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
        )
    }

    private fun collectNodes(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { addLast(root) }
        while (queue.isNotEmpty() && result.size < NodeTextCollector.MAX_NODES) {
            val node = queue.removeFirst()
            result += node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return result
    }
}

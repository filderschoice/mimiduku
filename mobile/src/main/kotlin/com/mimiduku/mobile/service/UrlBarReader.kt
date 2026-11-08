package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityNodeInfo
import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.TransitionTarget
import com.mimiduku.mobile.exclusion.DomainNormalizer
import com.mimiduku.mobile.exclusion.UrlBarIds

/**
 * 前面のブラウザのアドレスバーから、開いているサイトのドメインだけを読む。
 *
 * 読んだ文字列は [DomainNormalizer] でホスト名にして照合に使うだけで、そのまま保存・送信しない。
 * アドレスバーが見つからない・読めないブラウザでは null を返す（呼び出し側は確認を省かない）。
 */
object UrlBarReader {
    fun read(
        root: AccessibilityNodeInfo?,
        browserPackage: String,
    ): String? =
        root
            ?.takeIf { it.packageName?.toString() == browserPackage }
            ?.let { NodeTextCollector.walk(it) }
            ?.filter { UrlBarIds.isUrlBar(it.viewIdResourceName, browserPackage) }
            ?.firstNotNullOfOrNull { DomainNormalizer.fromUrlText(it.text?.toString()) }

    /** ブラウザへの遷移のときだけ、遷移先のブラウザのアドレスバーを読む。 */
    fun readTarget(
        root: AccessibilityNodeInfo?,
        transition: ExternalTransition,
    ): String? =
        transition.targetPackage
            .takeIf { transition.target == TransitionTarget.BROWSER }
            ?.let { read(root, it) }
}

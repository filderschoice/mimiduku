package com.mimiduku.mobile.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

/** 端末にあるアプリのうち、ブラウザ・ランチャー・キーボードを見分けるための問い合わせ。 */
object DevicePackages {
    fun resolve(
        context: Context,
        intent: Intent,
    ): Set<String> =
        context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

    fun inputMethods(context: Context): Set<String> =
        context
            .getSystemService(InputMethodManager::class.java)
            ?.inputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()
}

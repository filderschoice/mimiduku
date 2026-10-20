package com.mimiduku.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mimiduku.mobile.MainActivity
import com.mimiduku.mobile.R

/**
 * みみづくバッジを切り替える通知。ボタンを押すと、アプリを開かずにバッジを隠す・出すことができる。
 * 通知権限が無い（拒否された）ときは何も出さず、保護の動作には影響しない。
 */
class BadgeNotification(
    private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    /** 出している通知が表すバッジの表示状態。通知を出していなければ null。同じ状態を繰り返し出し直さないために持つ。 */
    private var shown: Boolean? = null

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    /** 設定とバッジの表示状態に合わせて、通知を出す・更新する・消す。 */
    fun sync(
        enabled: Boolean,
        badgeVisible: Boolean,
    ) {
        val target = badgeVisible.takeIf { enabled && manager.areNotificationsEnabled() }
        if (target == shown) return
        shown = target
        if (target == null) manager.cancel(NOTIFICATION_ID) else manager.notify(NOTIFICATION_ID, build(target))
    }

    fun cancel() {
        shown = null
        manager.cancel(NOTIFICATION_ID)
    }

    private fun build(badgeVisible: Boolean): Notification {
        val toggle =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_TOGGLE_BADGE).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val actionLabel =
            context.getString(
                if (badgeVisible) R.string.notification_action_hide else R.string.notification_action_show,
            )
        val status =
            context.getString(if (badgeVisible) R.string.notification_text_shown else R.string.notification_text_hidden)
        return Notification
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_owl)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(status)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, actionLabel, toggle).build())
            .build()
    }

    companion object {
        /** 通知のボタンから届く、バッジの表示を切り替える合図（自分のアプリの中だけに送る）。 */
        const val ACTION_TOGGLE_BADGE = "com.mimiduku.mobile.action.TOGGLE_BADGE"

        private const val CHANNEL_ID = "badge_control"
        private const val NOTIFICATION_ID = 1
    }
}

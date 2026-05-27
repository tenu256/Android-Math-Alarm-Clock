package com.alarm // ※ここはあなたのパッケージ名に合わせてください

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

// アラーム音をグローバルに（どこからでもアクセスできるように）保持する
object RingtonePlayer {
    var ringtone: android.media.Ringtone? = null
}

// ▼▼▼ 通知IDとチャンネルIDを定義 ▼▼▼
private const val ALARM_NOTIFICATION_ID = 1000
private const val ALARM_CHANNEL_ID = "alarm_channel"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. アラーム音を再生開始
        val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        RingtonePlayer.ringtone = RingtoneManager.getRingtone(context, alarmSoundUri)
        RingtonePlayer.ringtone?.play()

        // 2. 通知チャンネルを作成 (Android 8.0以降で必須)
        createNotificationChannel(context)

        // 3. MathChallengeActivityを起動するためのPendingIntentを作成
        val mathIntent = Intent(context, MathChallengeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            ALARM_NOTIFICATION_ID, // requestCode
            mathIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. 全画面インテントを持つ通知を構築
        val notificationBuilder = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // TODO: アラームアイコンに変更
            .setContentTitle("時間です！")
            .setContentText("問題を解いてアラームを止めてください")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 高優先度
            .setCategory(NotificationCompat.CATEGORY_ALARM) // アラームカテゴリ
            .setFullScreenIntent(fullScreenPendingIntent, true) // ★これが全画面インテント
            .setAutoCancel(true)

        // 5. 通知マネージャーで通知を発行
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALARM_NOTIFICATION_ID, notificationBuilder.build())
    }

    // ▼▼▼ 通知チャンネルを作成する関数 ▼▼▼
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "アラーム通知"
            val descriptionText = "アラーム作動時に使用されます"
            val importance = NotificationManager.IMPORTANCE_HIGH // ★最重要
            val channel = NotificationChannel(ALARM_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // ロック画面でも表示するように設定
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            // チャンネルをシステムに登録
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
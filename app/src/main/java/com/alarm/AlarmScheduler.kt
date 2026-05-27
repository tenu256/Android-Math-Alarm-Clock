package com.alarm // ← この行が MainActivity.kt と同じか確認！

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * OSにアラームを予約（スケジュール）します。
     */
    fun schedule(calendar: Calendar) {
        val intent = Intent(context, AlarmReceiver::class.java)

        // アラームを一意に識別するためのID
        val requestCode = calendar.timeInMillis.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 指定した時間に、スリープ状態でもOSを起動して実行するよう予約
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    /**
     * 予約したアラームをキャンセルします。
     */
    fun cancel(calendar: Calendar) {
        val intent = Intent(context, AlarmReceiver::class.java)

        // スケジュール時と全く同じID
        val requestCode = calendar.timeInMillis.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 予約をキャンセル
        alarmManager.cancel(pendingIntent)
    }
}
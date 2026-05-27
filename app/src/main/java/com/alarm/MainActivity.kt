package com.alarm // ※ここはあなたのパッケージ名に合わせてください

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences // ▼▼▼ 追加
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// SharedPreferencesのキー
private const val ALARM_PREFS = "AlarmPrefs"
private const val KEY_ALARM_TIME = "key_alarm_time"

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var scheduler: AlarmScheduler

    // ▼▼▼ 状態をActivityで管理 ▼▼▼
    private var alarmTime by mutableStateOf<Calendar?>(null)
    private var isAlarmSet by mutableStateOf(false)

    // 通知権限のリクエスター
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            toast(this, "通知権限がないためアラーム機能が使えません")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // SharedPreferencesとSchedulerを初期化
        prefs = getSharedPreferences(ALARM_PREFS, MODE_PRIVATE)
        scheduler = AlarmScheduler(this)

        // 権限確認
        askNotificationPermission()
        checkAndRequestAlarmPermission()

        setContent {
            MaterialTheme {
                // ▼▼▼ 状態とイベントハンドラをComposableに渡す ▼▼▼
                AlarmApp(
                    currentTime = rememberCurrentTime(), // 現在時刻はComposable内で管理
                    alarmTime = alarmTime,
                    isAlarmSet = isAlarmSet,
                    onSetAlarm = { calendar ->
                        setAlarm(calendar)
                    },
                    onCancelAlarm = {
                        cancelAlarm()
                    }
                )
            }
        }
    }

    // ▼▼▼ 画面が前面に戻るたびに、SharedPreferencesから状態を再読み込み ▼▼▼
    override fun onResume() {
        super.onResume()
        loadAlarmState()
    }

    private fun loadAlarmState() {
        val timeInMillis = prefs.getLong(KEY_ALARM_TIME, -1L)
        if (timeInMillis != -1L) {
            alarmTime = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
            isAlarmSet = true
        } else {
            alarmTime = null
            isAlarmSet = false
        }
    }

    // ▼▼▼ アラーム設定時のロジック ▼▼▼
    private fun setAlarm(calendar: Calendar) {
        // 1. OSにスケジュール
        scheduler.schedule(calendar)

        // 2. SharedPreferencesに保存
        prefs.edit().putLong(KEY_ALARM_TIME, calendar.timeInMillis).apply()

        // 3. 状態を更新
        loadAlarmState()

        val timeStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(calendar.time)
        toast(this, "$timeStr にアラームをセットしました")
    }

    // ▼▼▼ アラーム解除時のロジック ▼▼▼
    private fun cancelAlarm() {
        // 1. OSのスケジュールをキャンセル
        alarmTime?.let { scheduler.cancel(it) }

        // 2. SharedPreferencesから削除
        prefs.edit().remove(KEY_ALARM_TIME).apply()

        // 3. 状態を更新
        loadAlarmState()

        toast(this, "アラームを解除しました")
    }

    // --- 権限関連の関数 (変更なし) ---
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    private fun checkAndRequestAlarmPermission() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    startActivity(it)
                }
                toast(this, "アラームの許可が必要です。設定画面で許可してください。")
            }
        }
    }
}

// ▼▼▼ Composableは状態を持たず、表示とイベント通知に専念 ▼▼▼
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmApp(
    currentTime: Date,
    alarmTime: Calendar?,
    isAlarmSet: Boolean,
    onSetAlarm: (Calendar) -> Unit,
    onCancelAlarm: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("目覚まし時計アプリ") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentTime),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isAlarmSet && alarmTime != null) {
                Text(
                    text = "アラーム: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmTime.time)}",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCancelAlarm) { // 解除イベントを通知
                    Text("アラーム解除")
                }
            } else {
                Button(onClick = { showTimePicker = true }) {
                    Text("アラームを設定")
                }
            }
        }
    }

    // タイムピッカーダイアログ
    val timePickerState = rememberTimePickerState()
    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                // 権限チェック
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    toast(context, "アラーム権限が許可されていません。アプリを再起動して許可してください。")
                    showTimePicker = false
                } else {
                    // 設定イベントを通知
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    cal.set(Calendar.SECOND, 0)

                    if (cal.timeInMillis < System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    onSetAlarm(cal) // 設定イベントを通知
                    showTimePicker = false
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

// ▼▼▼ 現在時刻を更新し続けるComposable関数 ▼▼▼
@Composable
fun rememberCurrentTime(): Date {
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(key1 = Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }
    return currentTime
}

// タイムピッカーのガワ (変更なし)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("時刻を選択") },
        text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("キャンセル")
            }
        }
    )
}

// トースト (変更なし)
fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
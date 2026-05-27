package com.alarm // ※ここはあなたのパッケージ名に合わせてください

import android.os.Build // ▼▼▼ 追加
import android.os.Bundle
import android.view.WindowManager // ▼▼▼ 追加
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
class MathChallengeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ▼▼▼ このブロックをonCreateの最初に追加 ▼▼▼
        // 画面をロック解除し、スクリーンをオンにする
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // ▲▲▲

        val problem = generateMathProblem()

        setContent {
            MaterialTheme {
                var userAnswer by remember { mutableStateOf("") }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("アラームを止めてください！") }) }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = problem.first,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = userAnswer,
                            onValueChange = { userAnswer = it },
                            label = { Text("答え") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (userAnswer.toIntOrNull() == problem.second) {
                                    // 正解
                                    RingtonePlayer.ringtone?.stop()
                                    RingtonePlayer.ringtone = null

                                    val prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE)
                                    prefs.edit().remove("key_alarm_time").apply()

                                    finish()
                                } else {
                                    // 不正解
                                    userAnswer = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("回答", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }

    private fun generateMathProblem(): Pair<String, Int> {
        val num1 = (1..20).random()
        val num2 = (1..20).random()
        return Pair("$num1 + $num2 = ?", num1 + num2)
    }
}
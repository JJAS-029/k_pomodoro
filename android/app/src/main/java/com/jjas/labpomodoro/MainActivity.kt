package com.jjas.labpomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jjas.labpomodoro.ui.navigation.LabNavHost
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabPomodoroTheme {
                LabNavHost()
            }
        }
    }
}

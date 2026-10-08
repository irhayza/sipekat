package com.jargas.si_pekat

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jargas.si_pekat.ui.AppRoot
import com.jargas.si_pekat.ui.theme.SiPekatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar transparan dengan ikon terang (di atas header merah SiPEKAT).
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent { SiPekatTheme { AppRoot() } }
    }
}

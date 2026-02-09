package com.termux.app.x11

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.termux.app.ui.compose.theme.TermuxTheme
import com.termux.app.x11.ui.DesktopLauncherScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that shows the Desktop launcher/installation UI
 */
@AndroidEntryPoint
class X11LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            TermuxTheme {
                DesktopLauncherScreen()
            }
        }
    }
}

package com.penumbra.tv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.penumbra.tv.core.PenumbraController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PenumbraScreen() }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed in Settings; catch up the schedule and the overlay host.
        PenumbraController.sync(this)
    }
}

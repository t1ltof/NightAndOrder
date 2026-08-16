package com.nightandorder.game

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var brightness: BrightnessMonitor
    private lateinit var updates: UpdateClient
    private lateinit var game: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        brightness = BrightnessMonitor(this)
        brightness.start()
        updates = UpdateClient(this)
        updates.check()
        game = GameView(this, brightness, updates)
        setContentView(game)
    }

    override fun onResume() {
        super.onResume()
        brightness.refresh()
        updates.retryPendingInstall(this)
        game.onResumeGame()
    }

    override fun onPause() {
        game.onPauseGame()
        super.onPause()
    }

    override fun onDestroy() {
        brightness.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!game.onBack()) {
            super.onBackPressed()
        }
    }
}

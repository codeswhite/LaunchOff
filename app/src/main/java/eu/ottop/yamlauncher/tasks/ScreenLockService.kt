package eu.ottop.yamlauncher.tasks

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import eu.ottop.yamlauncher.utils.Logger

class ScreenLockService : AccessibilityService() {

    private lateinit var logger: Logger

    override fun onServiceConnected() {
        super.onServiceConnected()
        logger = Logger.getInstance(this)
        logger.i("ScreenLockService", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        logger.w("ScreenLockService", "Accessibility service interrupted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::logger.isInitialized) {
            logger = Logger.getInstance(this)
        }
        if (intent != null && intent.action == "LOCK_SCREEN") {
            logger.i("ScreenLockService", "Lock screen action received")
            performLockScreen()
        }
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun performLockScreen() {
        val success = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        if (success) {
            logger.i("ScreenLockService", "Screen locked successfully")
        } else {
            logger.e("ScreenLockService", "Failed to lock screen")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::logger.isInitialized) {
            logger.i("ScreenLockService", "Accessibility service destroyed")
        }
    }
}
package eu.ottop.yamlauncher.utils

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import eu.ottop.yamlauncher.R

class BiometricUtils(private val activity: FragmentActivity) {
    private lateinit var callbackSettings: CallbackSettings
    private val logger = Logger.getInstance(activity)

    interface CallbackSettings {
        fun onAuthenticationSucceeded()
        fun onAuthenticationFailed()
        fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?)
    }

    fun startBiometricSettingsAuth(callbackApp: CallbackSettings) {
        this.callbackSettings = callbackApp

        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                logger.i("BiometricUtils", "Biometric authentication succeeded")
                callbackSettings.onAuthenticationSucceeded()
            }

            override fun onAuthenticationFailed() {
                logger.w("BiometricUtils", "Biometric authentication failed")
                callbackSettings.onAuthenticationFailed()
            }

            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) {
                logger.e("BiometricUtils", "Biometric authentication error: $errorMessage (code: $errorCode)")
                callbackSettings.onAuthenticationError(errorCode, errorMessage)
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, authenticationCallback)

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val canAuthenticate =
            BiometricManager.from(activity).canAuthenticate(authenticators)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.text_biometric_login))
            .setSubtitle(activity.getString(R.string.text_biometric_login_sub))
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            logger.i("BiometricUtils", "Starting biometric authentication")
            biometricPrompt.authenticate(promptInfo)
        } else {
            logger.w("BiometricUtils", "Biometric authentication not available: $canAuthenticate")
        }
    }
}

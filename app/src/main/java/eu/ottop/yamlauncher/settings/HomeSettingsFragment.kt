package eu.ottop.yamlauncher.settings

import android.Manifest
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import eu.ottop.yamlauncher.R
import eu.ottop.yamlauncher.utils.PermissionUtils
import eu.ottop.yamlauncher.utils.UIUtils

class HomeSettingsFragment : PreferenceFragmentCompat(), TitleProvider {

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private val permissionUtils = PermissionUtils()

    private var gpsLocationPref: SwitchPreference? = null
    private var manualLocationPref: Preference? = null
    private var leftSwipePref: Preference? = null
    private var rightSwipePref: Preference? = null
    private var clockApp: Preference? = null
    private var dateApp: Preference? = null
    private var weatherIntervalPref: EditTextPreference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.home_preferences, rootKey)
            val uiUtils = UIUtils(requireContext())

            sharedPreferenceManager = SharedPreferenceManager(requireContext())

            clockApp = findPreference("clockSwipeApp")
            dateApp = findPreference("dateSwipeApp")

            gpsLocationPref = findPreference("gpsLocation")
            manualLocationPref = findPreference("manualLocation")
            leftSwipePref = findPreference("leftSwipeApp")
            rightSwipePref = findPreference("rightSwipeApp")
            weatherIntervalPref = findPreference("weatherUpdateInterval")

            // Only enable manual location when gps location is disabled
            if (gpsLocationPref != null && manualLocationPref != null) {
                manualLocationPref?.isEnabled = (gpsLocationPref?.isChecked == false)

                gpsLocationPref?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean && !permissionUtils.hasPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            (requireActivity() as SettingsActivity).requestLocationPermission()
                            return@OnPreferenceChangeListener false
                        } else {
                            manualLocationPref?.isEnabled = !newValue
                            return@OnPreferenceChangeListener true
                        }
                    }

                manualLocationPref?.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        uiUtils.switchFragment(requireActivity(), LocationFragment())
                        true
                    }
            }

            // Normalize weather update interval string (supports free-form: 10m / 6h / 1d)
            weatherIntervalPref?.let { pref ->
                val prefs = preferenceManager.sharedPreferences ?: return@let
                val existing = prefs.getString(pref.key, pref.text)
                val normalized = normalizeIntervalString(existing)
                if (normalized != null && normalized != existing) {
                    prefs.edit().putString(pref.key, normalized).apply()
                }

                pref.setOnPreferenceChangeListener { _, newValue ->
                    val normalizedNew = normalizeIntervalString(newValue as? String) ?: "15m"
                    prefs.edit().putString(pref.key, normalizedNew).apply()
                    false
                }
            }

            leftSwipePref?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    uiUtils.switchFragment(requireActivity(), GestureAppsFragment("left"))
                    true }

            rightSwipePref?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    uiUtils.switchFragment(requireActivity(), GestureAppsFragment("right"))
                    true }

            clockApp?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    uiUtils.switchFragment(requireActivity(), GestureAppsFragment("clock"))
                    true }

            dateApp?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    uiUtils.switchFragment(requireActivity(), GestureAppsFragment("date"))
                    true }
    }

    private fun normalizeIntervalString(raw: String?): String? {
        val s = raw?.trim()?.lowercase().orEmpty()
        if (s.isEmpty()) return null

        // Backward compatibility: stored as milliseconds (e.g. 600000)
        if (s.all { it.isDigit() }) {
            val ms = s.toLongOrNull() ?: return null
            val clamped = ms.coerceAtLeast(60_000L)
            return when {
                clamped % (24 * 60 * 60_000L) == 0L -> "${clamped / (24 * 60 * 60_000L)}d"
                clamped % (60 * 60_000L) == 0L -> "${clamped / (60 * 60_000L)}h"
                else -> "${clamped / 60_000L}m"
            }
        }

        val match = Regex("^(\\d+)\\s*([mhd])$").find(s) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        if (value <= 0L) return null

        return "$value$unit"
    }

    override fun onResume() {
        super.onResume()
        clockApp?.summary = sharedPreferenceManager.getGestureName("clock")

        dateApp?.summary = sharedPreferenceManager.getGestureName("date")

        manualLocationPref?.summary = sharedPreferenceManager.getWeatherRegion()

        leftSwipePref?.summary = sharedPreferenceManager.getGestureName("left")

        rightSwipePref?.summary = sharedPreferenceManager.getGestureName("right")
    }

    override fun getTitle(): String {
        return getString(R.string.home_settings_title)
    }

    fun setLocationPreference(isEnabled: Boolean) {
        manualLocationPref?.isEnabled = !isEnabled
        gpsLocationPref?.isChecked = isEnabled
    }
}

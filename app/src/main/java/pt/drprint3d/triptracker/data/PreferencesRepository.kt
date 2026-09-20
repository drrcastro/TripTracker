package pt.drprint3d.triptracker.data

import android.content.Context
import android.content.SharedPreferences

enum class LocationFormatType(val title: String) {
    GOOGLE_MAPS("Google Maps Link"),
    OPEN_STREET_MAP("OpenStreetMap Link"),
    GEO_URI("Geo URI (geo:lat,lng)"),
    COORDINATES("Coordenadas Simples"),
    CUSTOM("Formato Personalizado")
}

enum class OperatingMode(val title: String) {
    SINGLE_SMS("Single SMS"),
    LOOP_SMS("Loop SMS"),
    JUST_LOG("Just Log")
}

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("triptracker_prefs", Context.MODE_PRIVATE)

    var contactName: String
        get() = prefs.getString("contact_name", "") ?: ""
        set(value) = prefs.edit().putString("contact_name", value).apply()

    var contactPhone: String
        get() = prefs.getString("contact_phone", "") ?: ""
        set(value) = prefs.edit().putString("contact_phone", value).apply()

    var formatType: LocationFormatType
        get() {
            val name = prefs.getString("format_type", LocationFormatType.GOOGLE_MAPS.name)
            return try {
                LocationFormatType.valueOf(name ?: LocationFormatType.GOOGLE_MAPS.name)
            } catch (e: Exception) {
                LocationFormatType.GOOGLE_MAPS
            }
        }
        set(value) = prefs.edit().putString("format_type", value.name).apply()

    var customTemplate: String
        get() = prefs.getString("custom_template", "Localização: https://maps.google.com/?q={lat},{lng}") ?: ""
        set(value) = prefs.edit().putString("custom_template", value).apply()

    var intervalMinutes: Int
        get() = prefs.getInt("interval_minutes", 5)
        set(value) = prefs.edit().putInt("interval_minutes", value).apply()

    var operatingMode: OperatingMode
        get() {
            val name = prefs.getString("operating_mode", OperatingMode.SINGLE_SMS.name)
            return try {
                OperatingMode.valueOf(name ?: OperatingMode.SINGLE_SMS.name)
            } catch (e: Exception) {
                OperatingMode.SINGLE_SMS
            }
        }
        set(value) = prefs.edit().putString("operating_mode", value.name).apply()

    var appLanguage: String
        get() = prefs.getString("app_language", "pt") ?: "pt"
        set(value) = prefs.edit().putString("app_language", value).apply()
}

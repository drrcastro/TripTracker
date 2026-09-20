package pt.drprint3d.triptracker.utils

import pt.drprint3d.triptracker.data.LocationFormatType
import java.util.Locale

object FormatHelper {
    fun formatLocationMessage(
        latitude: Double,
        longitude: Double,
        formatType: LocationFormatType,
        customTemplate: String = ""
    ): String {
        val latStr = String.format(Locale.US, "%.6f", latitude)
        val lngStr = String.format(Locale.US, "%.6f", longitude)

        return when (formatType) {
            LocationFormatType.GOOGLE_MAPS -> {
                "Localização: https://maps.google.com/?q=$latStr,$lngStr"
            }
            LocationFormatType.OPEN_STREET_MAP -> {
                "Localização: https://www.openstreetmap.org/?mlat=$latStr&mlon=$lngStr#map=16/$latStr/$lngStr"
            }
            LocationFormatType.GEO_URI -> {
                "geo:$latStr,$lngStr"
            }
            LocationFormatType.COORDINATES -> {
                "Lat: $latStr, Lng: $lngStr"
            }
            LocationFormatType.CUSTOM -> {
                if (customTemplate.isBlank()) {
                    "Lat: $latStr, Lng: $lngStr"
                } else {
                    customTemplate
                        .replace("{lat}", latStr)
                        .replace("{lng}", lngStr)
                }
            }
        }
    }
}

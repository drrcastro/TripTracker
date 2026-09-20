package pt.drprint3d.triptracker.utils

import pt.drprint3d.triptracker.data.LocationLog
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GpxExporter {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun generateGpxXml(logs: List<LocationLog>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"TripTracker\"\n")
        sb.append("     xmlns=\"http://www.topografix.com/GPX/1/1\"\n")
        sb.append("     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
        sb.append("     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")

        sb.append("  <metadata>\n")
        sb.append("    <name>TripTracker Log Export</name>\n")
        sb.append("    <time>${isoDateFormat.format(Date())}</time>\n")
        sb.append("  </metadata>\n")

        sb.append("  <trk>\n")
        sb.append("    <name>TripTracker Track</name>\n")
        sb.append("    <trkseg>\n")

        for (log in logs) {
            val latStr = String.format(Locale.US, "%.6f", log.latitude)
            val lonStr = String.format(Locale.US, "%.6f", log.longitude)
            val timeStr = isoDateFormat.format(Date(log.timestamp))

            sb.append("      <trkpt lat=\"$latStr\" lon=\"$lonStr\">\n")
            if (log.altitude != null && log.altitude != 0.0) {
                val eleStr = String.format(Locale.US, "%.2f", log.altitude)
                sb.append("        <ele>$eleStr</ele>\n")
            }
            sb.append("        <time>$timeStr</time>\n")

            val recipient = if (!log.recipientPhone.isNull_or_empty()) {
                val name = log.recipientName ?: ""
                "$name (${log.recipientPhone})"
            } else "Nenhum"

            val desc = "Modo: ${log.mode} | Contacto: $recipient | SMS enviada: ${if (log.smsSent) "Sim" else "Não"}"
            sb.append("        <desc>${escapeXml(desc)}</desc>\n")
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    fun exportToStream(logs: List<LocationLog>, outputStream: OutputStream) {
        val xmlText = generateGpxXml(logs)
        outputStream.write(xmlText.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    private fun CharSequence?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

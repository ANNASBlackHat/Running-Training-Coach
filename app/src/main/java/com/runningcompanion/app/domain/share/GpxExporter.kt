package com.runningcompanion.app.domain.share

import com.runningcompanion.app.domain.model.GpsPoint
import com.runningcompanion.app.domain.model.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Serializes a [Session] to a GPX 1.1 document (track + start/finish
 * waypoints), suitable for import into other training platforms.
 */
object GpxExporter {

    private val isoUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    fun export(session: Session): String {
        val start = session.track.firstOrNull()
        val end = session.track.lastOrNull()
        val name = session.workoutSnapshot.name

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Running Companion\" ")
        sb.append("xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escape(name)).append("</name>\n")
        sb.append("    <time>").append(isoUtc.format(Date(session.startedAt))).append("</time>\n")
        sb.append("  </metadata>\n")

        if (start != null) {
            sb.append(waypoint(start, name + " (start)"))
        }
        if (end != null && start != null &&
            (start.latitude != end.latitude || start.longitude != end.longitude)
        ) {
            sb.append(waypoint(end, name + " (finish)"))
        }

        sb.append("  <trk>\n")
        sb.append("    <name>").append(escape(name)).append("</name>\n")
        sb.append("    <trkseg>\n")
        for (point in session.track) {
            sb.append("      <trkpt lat=\"").append(point.latitude).append("\" ")
                .append("lon=\"").append(point.longitude).append("\">\n")
            point.altitude?.let {
                sb.append("        <ele>").append(it).append("</ele>\n")
            }
            sb.append("        <time>").append(isoUtc.format(Date(point.timestamp))).append("</time>\n")
            sb.append("      </trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    private fun waypoint(point: GpsPoint, name: String): String {
        val sb = StringBuilder()
        sb.append("  <wpt lat=\"").append(point.latitude).append("\" ")
            .append("lon=\"").append(point.longitude).append("\">\n")
        sb.append("    <name>").append(escape(name)).append("</name>\n")
        sb.append("    <time>").append(isoUtc.format(Date(point.timestamp))).append("</time>\n")
        sb.append("  </wpt>\n")
        return sb.toString()
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

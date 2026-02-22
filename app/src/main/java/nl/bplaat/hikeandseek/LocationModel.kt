/*
 * Copyright (c) 2026 Bastiaan van der Plaat
 *
 * SPDX-License-Identifier: MIT
 */

package nl.bplaat.hikeandseek

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan
import kotlin.math.sqrt

data class Location(
    val id: String,
    val rdX: Double,
    val rdY: Double,
    val description: String
) {
    val latitude: Double
        get() = rdToWgs84(rdX, rdY).first

    val longitude: Double
        get() = rdToWgs84(rdX, rdY).second
}

object LocationParser {
    fun parseLocations(text: String): List<Location> {
        val locations = mutableListOf<Location>()

        // First, clean up the text: normalize whitespace and fix common OCR errors
        var cleanText = text.replace("\r", "").trim()

        // Remove common OCR artifacts
        cleanText = cleanText
            // Fix common OCR confusions
            .replace("\\|", "I")  // Pipe might be detected as I
            .replace("O ", "0 ")  // Letter O at start of coordinate might be 0
            .replace(" O ", " 0 ")  // Letter O in middle
            // Clean up multiple spaces/newlines
            .replace("  +", " ")  // Multiple spaces to single

        // More robust regex pattern that handles:
        // - ID (number, letter, or with *)
        // - Optional whitespace around colons
        // - RD coordinates (4-5 digits each, may have OCR errors)
        // - Description that can span multiple lines
        // Pattern: ID : RD_X RD_Y : description
        val pattern = """([A-Za-z0-9]+\*?)\s*:\s*(\d{4,5})\s+(\d{4,5})\s*:\s*([^\n]*(?:\n(?![A-Za-z0-9*]+\s*:)[^\n]*)*)""".toRegex(
            RegexOption.MULTILINE
        )

        val matches = pattern.findAll(cleanText)

        for (match in matches) {
            try {
                val id = match.groupValues[1].trim()
                val rdX = match.groupValues[2].toDouble()
                val rdY = match.groupValues[3].toDouble()
                var description = match.groupValues[4].trim()

                // Clean up description: consolidate whitespace and newlines
                description = description
                    .replace("\n", " ")  // Replace newlines with space
                    .replace("\\s+".toRegex(), " ")  // Normalize multiple spaces
                    .trim()

                // Skip invalid entries
                if (id.isEmpty() || description.isEmpty() || !isValidCoordinate(rdX, rdY)) {
                    continue
                }

                locations.add(Location(id, rdX, rdY, description))
            } catch (e: Exception) {
                // Skip malformed entries
            }
        }

        return locations
    }

    private fun isValidCoordinate(x: Double, y: Double): Boolean {
        // Dutch RD coordinates are roughly in range 0-350000
        return x in 0.0..350000.0 && y in 0.0..350000.0
    }
}

// RD (Rijksdriehoek) to WGS84 (lat/lon) conversion
// Based on EPSG:28992 (RD) to EPSG:4326 (WGS84) transformation
// Uses the standard Dutch geodetic reference coefficients
fun rdToWgs84(rdX: Double, rdY: Double): Pair<Double, Double> {
    // Normalize coordinates (use the official reference point)
    val pX = (rdX - 155000.0) / 100000.0
    val pY = (rdY - 463000.0) / 100000.0

    // Calculate latitude (proper coefficients for Dutch RD system)
    val lat = 52.15517440 +
            3.23996875 * pY -
            0.12481899 * pX +
            0.24477037 * pX * pX -
            0.06663307 * pY * pY +
            0.20938589 * pX * pY +
            0.00014510 * pX * pX * pX +
            0.00283793 * pY * pY * pY -
            0.00022936 * pX * pX * pY -
            0.00629124 * pX * pY * pY

    // Calculate longitude (proper coefficients for Dutch RD system)
    val lon = 4.73477327 +
            1.04700284 * pX -
            0.31533751 * pY -
            0.08786691 * pX * pX -
            0.01179502 * pY * pY -
            0.00115964 * pX * pY -
            0.00011515 * pX * pX * pX +
            0.00125060 * pY * pY * pY -
            0.00014530 * pX * pX * pY -
            0.00084159 * pX * pY * pY

    return Pair(lat, lon)
}

fun generateGPX(locations: List<Location>): String {
    val sb = StringBuilder()
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())

    sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="HikeAndSeek"
    xmlns="http://www.topografix.com/GPX/1/1"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">desc
  <metadata>
    <name>HikeAndSeek Locations</name>
    <time>${timestamp}</time>
  </metadata>
""")

    for (location in locations) {
        sb.append("""  <wpt lat="${location.latitude}" lon="${location.longitude}">
    <name>${location.id}</name>
    <desc>${location.description}</desc>
    <extensions>
      <rdX>${location.rdX}</rdX>
      <rdY>${location.rdY}</rdY>
    </extensions>
  </wpt>
""")
    }

    sb.append("</gpx>")
    return sb.toString()
}

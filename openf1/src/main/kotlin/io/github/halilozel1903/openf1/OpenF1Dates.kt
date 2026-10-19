package io.github.halilozel1903.openf1

import kotlin.time.Instant

/** Reads OpenF1 timestamps, such as `2025-12-07T13:03:27.584000+00:00` or `2023-06-04T14:21:01+00:00`. */
public object OpenF1Dates {
    /** The instant [text] names, or `null` when it is `null` or not ISO 8601. */
    public fun parse(text: String?): Instant? {
        if (text.isNullOrBlank()) return null
        val normalized = text.trim().let { if (hasZone(it)) it else "${it}Z" }
        return try {
            Instant.parse(normalized)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun hasZone(text: String): Boolean {
        val time = text.substringAfter('T', missingDelimiterValue = "")
        return text.endsWith('Z') || text.endsWith('z') || '+' in time || '-' in time
    }
}

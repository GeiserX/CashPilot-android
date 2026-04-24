package com.cashpilot.android

import com.cashpilot.android.util.FormatUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Parameterized tests for FormatUtils to cover all branches systematically.
 */
class FormatUtilsParameterizedTest {

    @ParameterizedTest(name = "formatBytes({0}) = {1}")
    @CsvSource(
        "0, 0 B",
        "1, 1 B",
        "100, 100 B",
        "512, 512 B",
        "1023, 1023 B",
        "1024, 1 KB",
        "1025, 1 KB",
        "2048, 2 KB",
        "524288, 512 KB",
        "1048575, 1023 KB",
        "1048576, 1.0 MB",
        "1572864, 1.5 MB",
        "5242880, 5.0 MB",
        "104857600, 100.0 MB",
        "1073741823, 1024.0 MB",
        "1073741824, 1.00 GB",
        "2147483648, 2.00 GB",
        "10737418240, 10.00 GB",
    )
    fun `formatBytes parameterized`(bytes: Long, expected: String) {
        assertEquals(expected, FormatUtils.formatBytes(bytes))
    }

    @ParameterizedTest(name = "parseIso({0}) should return positive millis")
    @ValueSource(
        strings = [
            "2024-01-01T00:00:01Z",
            "2024-06-15T12:30:45Z",
            "2026-04-25T23:59:59Z",
            "2024-12-31T23:59:59.999Z",
            "2000-01-01T00:00:00Z",
        ]
    )
    fun `parseIso valid timestamps return positive millis`(iso: String) {
        assertTrue(FormatUtils.parseIso(iso) > 0, "Expected positive millis for $iso")
    }

    @ParameterizedTest(name = "parseIso({0}) should return 0")
    @ValueSource(
        strings = [
            "",
            "not-a-date",
            "2024-01-15",
            "12:30:00",
            "yesterday",
            "2024/01/15T10:30:00Z",
            "null",
        ]
    )
    fun `parseIso invalid timestamps return zero`(iso: String) {
        assertEquals(0L, FormatUtils.parseIso(iso), "Expected 0 for invalid: $iso")
    }
}

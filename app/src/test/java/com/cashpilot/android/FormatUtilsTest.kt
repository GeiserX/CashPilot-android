package com.cashpilot.android

import com.cashpilot.android.util.FormatUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FormatUtilsTest {

    // --- formatBytes ---

    @Test
    fun `formatBytes zero bytes`() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
    }

    @Test
    fun `formatBytes small bytes`() {
        assertEquals("512 B", FormatUtils.formatBytes(512))
    }

    @Test
    fun `formatBytes one byte`() {
        assertEquals("1 B", FormatUtils.formatBytes(1))
    }

    @Test
    fun `formatBytes just under 1 KB`() {
        assertEquals("1023 B", FormatUtils.formatBytes(1023))
    }

    @Test
    fun `formatBytes exactly 1 KB`() {
        assertEquals("1 KB", FormatUtils.formatBytes(1024))
    }

    @Test
    fun `formatBytes kilobytes`() {
        assertEquals("500 KB", FormatUtils.formatBytes(512000))
    }

    @Test
    fun `formatBytes just under 1 MB`() {
        // 1048575 bytes = 1024 KB - 1 byte = 1023 KB
        assertEquals("1023 KB", FormatUtils.formatBytes(1024 * 1024 - 1))
    }

    @Test
    fun `formatBytes exactly 1 MB`() {
        assertEquals("1.0 MB", FormatUtils.formatBytes(1024 * 1024))
    }

    @Test
    fun `formatBytes megabytes`() {
        assertEquals("5.0 MB", FormatUtils.formatBytes(5L * 1024 * 1024))
    }

    @Test
    fun `formatBytes fractional megabytes`() {
        assertEquals("1.5 MB", FormatUtils.formatBytes((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formatBytes just under 1 GB`() {
        val justUnder1GB = 1024L * 1024 * 1024 - 1
        assertTrue(FormatUtils.formatBytes(justUnder1GB).endsWith("MB"))
    }

    @Test
    fun `formatBytes exactly 1 GB`() {
        assertEquals("1.00 GB", FormatUtils.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun `formatBytes gigabytes`() {
        assertEquals("2.50 GB", FormatUtils.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formatBytes large gigabytes`() {
        assertEquals("100.00 GB", FormatUtils.formatBytes(100L * 1024 * 1024 * 1024))
    }

    // --- parseIso ---

    @Test
    fun `parseIso valid ISO instant`() {
        val millis = FormatUtils.parseIso("2024-01-15T10:30:00Z")
        assertTrue(millis > 0)
        assertEquals(1705314600000L, millis)
    }

    @Test
    fun `parseIso valid ISO with fractional seconds`() {
        val millis = FormatUtils.parseIso("2024-01-15T10:30:00.500Z")
        assertEquals(1705314600500L, millis)
    }

    @Test
    fun `parseIso empty string returns zero`() {
        assertEquals(0L, FormatUtils.parseIso(""))
    }

    @Test
    fun `parseIso invalid string returns zero`() {
        assertEquals(0L, FormatUtils.parseIso("not-a-date"))
    }

    @Test
    fun `parseIso random garbage returns zero`() {
        assertEquals(0L, FormatUtils.parseIso("abcdef12345"))
    }

    @Test
    fun `parseIso partial ISO returns zero`() {
        assertEquals(0L, FormatUtils.parseIso("2024-01-15"))
    }

    @Test
    fun `parseIso epoch start`() {
        assertEquals(0L, FormatUtils.parseIso("1970-01-01T00:00:00Z"))
    }

    @Test
    fun `parseIso recent timestamp`() {
        val millis = FormatUtils.parseIso("2026-04-24T12:00:00Z")
        assertTrue(millis > 1700000000000L)
    }
}

package com.yunseong.phonemonitor

import com.yunseong.phonemonitor.data.CoreFreq
import com.yunseong.phonemonitor.data.CpuStats
import com.yunseong.phonemonitor.data.MemoryStats
import com.yunseong.phonemonitor.data.StorageStats
import com.yunseong.phonemonitor.data.formatBytes
import com.yunseong.phonemonitor.data.formatKhz
import com.yunseong.phonemonitor.data.formatRate
import com.yunseong.phonemonitor.widget.asciiBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatTest {
    init {
        Locale.setDefault(Locale.US)
    }

    @Test fun bytes() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.5 GB", formatBytes(1536L * 1024 * 1024))
        assertEquals("0 B", formatBytes(0))
    }

    @Test fun rate() {
        assertNull(formatRate(null))
        assertEquals("2.0 MB/s", formatRate(2L * 1024 * 1024))
    }

    @Test fun khz() {
        assertEquals("1.80 GHz", formatKhz(1_800_000))
        assertEquals("600 MHz", formatKhz(600_000))
    }

    @Test fun asciiBarClampsAndRounds() {
        assertEquals("░░░░░", asciiBar(0, 5))
        assertEquals("█████", asciiBar(100, 5))
        assertEquals("███░░", asciiBar(55, 5))
        assertEquals("█████", asciiBar(150, 5))
        assertEquals("░░░░░", asciiBar(-10, 5))
    }

    @Test fun percentWithZeroTotal() {
        // 읽기 실패 시 기본값(0)이어도 0으로 나누지 않는다
        assertEquals(0, MemoryStats(0, 0, false).usedPercent)
        assertEquals(0, StorageStats(0, 0).usedPercent)
        assertEquals(50, StorageStats(100, 50).usedPercent)
    }

    @Test fun cpuWithNoReadableCores() {
        assertTrue(CpuStats(emptyList()).unavailable)
        assertTrue(CpuStats(listOf(CoreFreq(0, null, null))).unavailable)
        assertEquals(2_000_000L, CpuStats(listOf(CoreFreq(0, 1_000_000, null), CoreFreq(1, 2_000_000, null))).fastestKhz)
    }
}

package com.yunseong.phonemonitor

import com.yunseong.phonemonitor.cleanup.Cleanup
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupTest {
    private fun first(manufacturer: String) = Cleanup.targets(manufacturer).first().label

    @Test fun vendorScreenComesFirst() {
        assertEquals(R.string.cleanup_samsung_ram, first("samsung"))
        assertEquals(R.string.cleanup_xiaomi, first("Xiaomi"))
        assertEquals(R.string.cleanup_xiaomi, first("POCO"))
        assertEquals(R.string.cleanup_phone_manager, first("HONOR"))
        assertEquals(R.string.cleanup_phone_manager, first("OnePlus"))
        assertEquals(R.string.cleanup_vivo, first("vivo"))
    }

    @Test fun otherPhonesUseStorageScreen() {
        assertEquals(R.string.cleanup_storage, first("Google"))
        assertEquals(R.string.cleanup_storage, first("motorola"))
    }

    @Test fun storageScreenIsAlwaysTheFallback() {
        for (m in listOf("samsung", "xiaomi", "Google")) {
            assertEquals(R.string.cleanup_storage_settings, Cleanup.targets(m).last().label)
        }
    }
}

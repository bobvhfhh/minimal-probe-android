package com.minprobe.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    @Test
    fun updateIsAvailableOnlyWhenRemoteVersionIsNewer() {
        assertTrue(UpdatePolicy.isNewer(7, 6))
        assertFalse(UpdatePolicy.isNewer(6, 6))
        assertFalse(UpdatePolicy.isNewer(5, 6))
    }

    @Test
    fun apkFileNameUsesSafeVersionAndReleaseLabel() {
        assertEquals("MinimalProbe-v1.2.2-release.apk", UpdatePolicy.apkFileName("1.2.2"))
        assertEquals("MinimalProbe-v1-2-2-release.apk", UpdatePolicy.apkFileName("1/2/2"))
    }

    @Test
    fun updateDownloadUrlMustUseHttps() {
        assertTrue(UpdatePolicy.isSecureDownloadUrl("https://example.com/app.apk"))
        assertFalse(UpdatePolicy.isSecureDownloadUrl("http://example.com/app.apk"))
        assertFalse(UpdatePolicy.isSecureDownloadUrl("not a url"))
    }
}

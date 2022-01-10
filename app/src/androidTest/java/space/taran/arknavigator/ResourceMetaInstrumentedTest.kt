package space.taran.arknavigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arknavigator.mvp.model.repo.index.computeId
import space.taran.arknavigator.ui.activity.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@RunWith(AndroidJUnit4::class)
class ResourceMetaTest {
    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
    @Test
    fun crc32_iscorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(appContext.cacheDir.toString() + "/android-logo-mask.png")
        var size: Long = f.length();
        if (!f.exists()) try {
            val `is`: InputStream =  appContext.assets.open("images/android-logo-mask.png")
            size = `is`.available().toLong()
            val buffer = ByteArray(size.toInt())
            `is`.read(buffer)
            `is`.close()
            val fos = FileOutputStream(f)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        assertEquals(computeId(size, f.toPath()),0xe4c0951c);
    }
}
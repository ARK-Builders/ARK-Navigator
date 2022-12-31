package space.taran.arknavigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arklib.computeId
import space.taran.arknavigator.ui.activity.MainActivity

@RunWith(AndroidJUnit4::class)
class ResourceMetaTest {
    @get:Rule
    val mainActivityRule = ActivityTestRule(MainActivity::class.java)
    @Test
    fun crc32_iscorrect() {
        val appContext = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val file = File(
            appContext.cacheDir.toString() +
                "/android-logo-mask.png"
        )

        var size: Long = file.length()
        if (!file.exists()) try {
            val `is`: InputStream = appContext.assets
                .open("images/android-logo-mask.png")

            size = `is`.available().toLong()
            val buffer = ByteArray(size.toInt())
            `is`.read(buffer)
            `is`.close()

            val fos = FileOutputStream(file)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        assertEquals(computeId(size, file.toPath()).crc32, 0xe4c0951c)
    }
}

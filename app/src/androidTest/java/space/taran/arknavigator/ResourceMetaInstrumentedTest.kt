package space.taran.arknavigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import space.taran.arknavigator.mvp.model.repo.index.computeId
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.FileSystems

@RunWith(AndroidJUnit4::class)
class ResourceMetaTest {
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
        assertEquals(computeId(
            size = size,
            file = f.toPath()
        ), 0xe4c0951c);
    }
}
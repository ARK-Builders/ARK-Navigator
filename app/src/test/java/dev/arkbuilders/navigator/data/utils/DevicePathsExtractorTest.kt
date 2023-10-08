package dev.arkbuilders.navigator.data.utils


import dev.arkbuilders.navigator.presentation.App
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths


@RunWith(MockitoJUnitRunner::class)
class DevicePathsExtractorTest {

    private val mockedApplication = mockk<App>()

    private lateinit var testee: DevicePathsExtractor

    @BeforeEach
    fun setUp() {
        testee = DevicePathsExtractorImpl(
            appInstance = mockedApplication
        )
    }

    @Test
    fun givenApplicationAvailable_whenGetExternalFileDirs_thenReturnCorrectResult() {
        val mockedFile = mockk<File>()
        val files = listOf(mockedFile)
        every { mockedApplication.getExternalFilesDirs(null) } returns files.toTypedArray()
        every { mockedFile.exists() } returns true

        val mockedPath = mockk<Path>()
        every { mockedFile.toPath() } returns mockedPath
        every { mockedPath.toRealPath() } returns mockedPath

        val directoryIterator: MutableIterator<Path> = arrayListOf(
            Paths.get("notAndroid")

        ).iterator()
        every { mockedPath.iterator() } returns directoryIterator

        testee.listDevices()

        verify { mockedApplication.getExternalFilesDirs(null).toList() }
    }
}

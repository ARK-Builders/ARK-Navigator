package dev.arkbuilders.navigator.data.utils

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.notExists

@RunWith(MockitoJUnitRunner::class)
class PathExtKtTest {

    private lateinit var testee: Path

    @BeforeEach
    fun setUp() {
        testee = Paths.get("PATH")
    }

    @Test
    fun givenPath_whenFindNotExistCopyNameFalse_thenReturnNewPath() {
        val sourcePath = kotlin.io.path.createTempFile("PATH")
        val destPath = mockk<Path>()

        every { destPath.fileName } returns Paths.get("PATH")
        val result = sourcePath.findNotExistCopyName(destPath)

        assertEquals("PATH_1.", result.name)
    }

    @Test
    fun givenPath_whenFindNotExistCopyNameTrue_thenReturnOriginalPath() {
        val mockedPath = mockk<Path>()
        every { mockedPath.notExists() } returns true

        val result = testee.findNotExistCopyName(Paths.get("PATH"))

        assertEquals("PATH", result.name)
    }

    @Test
    fun whenFindLongestCommonPrefixAndPathsEmpty_thenThrowException() {
        assertThrows(IllegalArgumentException::class.java) {
            findLongestCommonPrefix(emptyList())
        }
    }

    @Test
    fun whenFindLongestCommonPrefixAndOneItem_thenReturnFirstItem() {
        val result = findLongestCommonPrefix(listOf(Paths.get("PATH")))
        assertEquals("PATH", result.name)
    }

    @Test
    fun whenFindLongestCommonPrefixAndMoreThanOnePrefixGroup_thenReturnFirstPairOfPrefixAndPath() {
        val result = findLongestCommonPrefix(
            listOf(
                Paths.get("PATH"),
                Paths.get("PATH2")
            )
        )
        assertEquals("/", result.toString())
    }

    @Test
    fun whenFindLongestCommonPrefixAndOnePrefixGroup_thenReturnFirstRelativePairOfPrefixAndPath() {
        assertThrows(IllegalArgumentException::class.java) {
            findLongestCommonPrefix(
                listOf(
                    Paths.get("/PATH/"),
                    Paths.get("/PATH")
                )
            )
        }
    }
}

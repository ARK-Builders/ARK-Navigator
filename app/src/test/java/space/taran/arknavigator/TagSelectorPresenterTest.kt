package space.taran.arknavigator

import android.util.Log
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.QueryMode
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagItem
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorAction
import space.taran.arknavigator.mvp.presenter.adapter.tagsselector.TagsSelectorPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.stub.R1
import space.taran.arknavigator.stub.R2
import space.taran.arknavigator.stub.R3
import space.taran.arknavigator.stub.R4
import space.taran.arknavigator.stub.ResourceIndexStub
import space.taran.arknavigator.stub.TAG1
import space.taran.arknavigator.stub.TAG2
import space.taran.arknavigator.stub.TagsStorageStub

@ExperimentalCoroutinesApi
@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagSelectorPresenterTest {

    @RelaxedMockK
    private lateinit var viewState: ResourcesView

    private val dispatcher = StandardTestDispatcher()
    private val scope = CoroutineScope(dispatcher)

    private lateinit var presenter: TagsSelectorPresenter
    private lateinit var storage: TagsStorage
    private lateinit var index: ResourcesIndex

    @BeforeAll
    fun beforeAll() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @BeforeEach
    fun beforeEach() {
        presenter = TagsSelectorPresenter(
            viewState,
            null,
            scope,
            ::onSelectionChange
        )
        index = ResourceIndexStub()
        storage = TagsStorageStub()
        presenter.init(index, storage, false)
    }

    @Test
    fun `selection should contain only resources that contain the selected tags`() =
        runTest(dispatcher) {
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1))
                .join()
            assertEquals(presenter.selection, setOf(R1))
        }

    @Test
    fun `selection should contain only resources that contain the selected tags, 2nd case`() =
        runTest(dispatcher) {
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG2)).join()
            assertEquals(presenter.selection, setOf(R1))
        }

    @Test
    fun `second click on tag reverts selection state`() =
        runTest(dispatcher) {
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            assertEquals(presenter.selection, setOf(R1, R2, R3, R4))
        }

    @Test
    fun `selection should contain only resources without the excluded tag`() =
        runTest(dispatcher) {
            presenter.onTagItemLongClick(TagItem.PlainTagItem(TAG1)).join()
            assertEquals(presenter.selection, setOf(R2, R3, R4))
        }

    @Test
    fun `selection should contain only resources without tags when no tag is selected in the focus mode`() =
        runTest(dispatcher) {
            presenter.onQueryModeChanged(QueryMode.FOCUS).join()
            assertEquals(presenter.selection, setOf(R3))
        }

    @Test
    fun `selection should contain only resources with selected tags in the focus mode`() =
        runTest(dispatcher) {
            presenter.onQueryModeChanged(QueryMode.FOCUS).join()
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG2)).join()
            assertEquals(presenter.selection, setOf(R2))
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            assertEquals(presenter.selection, setOf(R1))
        }

    @Test
    fun `back click should return the previous selection`() = runTest(dispatcher) {
        val tagItem1 = TagItem.PlainTagItem(TAG2)
        val tagItem2 = TagItem.PlainTagItem(TAG1)
        presenter.onTagItemClick(tagItem1).join()
        assertEquals(presenter.selection, setOf(R1, R2))
        presenter.onTagItemClick(tagItem2).join()
        val actionInclude1 = TagsSelectorAction.Include(tagItem1)
        val actionInclude2 = TagsSelectorAction.Include(tagItem2)
        assertEquals(
            presenter.actionsHistory,
            listOf(actionInclude1, actionInclude2)
        )
        assertEquals(presenter.selection, setOf(R1))
        presenter.onBackClick()
        assertEquals(presenter.actionsHistory, listOf(actionInclude1))
        assertEquals(presenter.selection, setOf(R1, R2))
    }

    @Test
    fun `removing the selected tag should change the selection`() =
        runTest(dispatcher) {
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            storage.setTagsAndPersist(R1, setOf())
            presenter.calculateTagsAndSelection()
            assertEquals(presenter.selection, setOf(R1, R2, R3, R4))
        }

    @Test
    fun `selection should contain all the resources after the clear button has been clicked`() =
        runTest(dispatcher) {
            presenter.onTagItemClick(TagItem.PlainTagItem(TAG1)).join()
            presenter.onClearClick().join()
            assertEquals(presenter.selection, setOf(R1, R2, R3, R4))
        }

    private suspend fun onSelectionChange(resources: Set<ResourceId>) {}
}

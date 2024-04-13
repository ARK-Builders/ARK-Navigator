package dev.arkbuilders.navigator.presentation.screen.gallery.galleryuplift.domain

import dev.arkbuilders.arkfilepicker.folders.RootAndFav
import dev.arkbuilders.arklib.ResourceId
import dev.arkbuilders.arklib.data.index.ResourceIndex
import dev.arkbuilders.arklib.user.tags.TagStorage
import dev.arkbuilders.navigator.data.stats.StatsStorage

data class ShowEditTagsData(
    val resource: ResourceId,
    val rootAndFav: RootAndFav,
    val resources: List<ResourceId>,
    val index: ResourceIndex,
    val storage: TagStorage,
    val statsStorage: StatsStorage
)

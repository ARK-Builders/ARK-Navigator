package space.taran.arknavigator.mvp.model.repo.index

import java.nio.file.Path

interface IndexFailedPathCallback {
    fun indexFailed(path: Path)
}

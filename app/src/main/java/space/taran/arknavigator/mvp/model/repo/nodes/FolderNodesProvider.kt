package space.taran.arknavigator.mvp.model.repo.nodes

import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FolderNode

interface FolderNodesProvider {
    fun save(nodes: List<FolderNode>)
    fun provide(): List<FolderNode>?
}

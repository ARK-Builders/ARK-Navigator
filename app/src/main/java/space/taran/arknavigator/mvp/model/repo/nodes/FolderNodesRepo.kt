package space.taran.arknavigator.mvp.model.repo.nodes

import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FolderNode

class FolderNodesRepo(
    private val nodesProvider: FolderNodesProvider
) {
    fun save(nodes: List<FolderNode>) = nodesProvider.save(nodes)

    fun provide() = nodesProvider.provide()
}

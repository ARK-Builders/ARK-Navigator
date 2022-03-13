package space.taran.arknavigator.mvp.model.repo.nodes

import space.taran.arknavigator.mvp.presenter.adapter.folderstree.FolderNode

class ListFolderNodesProvider : FolderNodesProvider {
    private var nodes: List<FolderNode>? = null

    override fun save(nodes: List<FolderNode>) {
        this.nodes = nodes
    }

    override fun provide(): List<FolderNode>? {
        return nodes
    }
}

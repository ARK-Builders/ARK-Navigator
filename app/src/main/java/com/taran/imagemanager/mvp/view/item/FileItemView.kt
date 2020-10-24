package com.taran.imagemanager.mvp.view.item

import com.taran.imagemanager.mvp.model.entity.Icons

interface FileItemView {
    var pos: Int

    fun setIcon(resourceType: Icons, path: String?)
    fun setText(title: String)
}
package com.taran.imagemanager.mvp.model.file

import com.taran.imagemanager.mvp.model.entity.Image

interface FileProvider {
    fun getImagesFromGallery(): List<Image>
    fun getExternalStorage(): String
}
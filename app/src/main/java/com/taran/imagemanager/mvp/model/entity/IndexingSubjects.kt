package com.taran.imagemanager.mvp.model.entity

import io.reactivex.rxjava3.subjects.ReplaySubject

class IndexingSubjects {
    val map = hashMapOf<String, ReplaySubject<Image>>()
}
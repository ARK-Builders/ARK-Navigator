package com.taran.imagemanager.mvp.model.entity

import io.reactivex.rxjava3.subjects.ReplaySubject

class ActiveIndexingStorage {
    val map = HashMap<String, ReplaySubject<Boolean>>()
}
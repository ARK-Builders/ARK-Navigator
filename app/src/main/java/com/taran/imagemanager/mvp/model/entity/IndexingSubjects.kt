package space.taran.arkbrowser.mvp.model.entity

import io.reactivex.rxjava3.subjects.ReplaySubject

class IndexingSubjects {
    val map = hashMapOf<String, ReplaySubject<Image>>()
}
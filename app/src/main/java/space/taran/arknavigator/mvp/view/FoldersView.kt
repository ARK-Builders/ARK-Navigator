package space.taran.arknavigator.mvp.view

import moxy.MvpView
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.StateStrategyType
import space.taran.arknavigator.mvp.model.repo.Folders

@StateStrategyType(AddToEndSingleStrategy::class)
interface FoldersView: MvpView, NotifiableView {
    fun loadFolders(folders: Folders)

//    @StateStrategyType(SkipStrategy::class)
//    fun requestSdCardUri()


//todo
//    private fun storageVersionDifferent(fileStorageVersion: Int, root: remove_Root) {
//        viewState.notifyUser("${root.storage.path} has a different version")
//    }

//    override fun requestSdCardUri() {
//        Log.d(ROOTS_SCREEN, "requesting sd card URI in RootsFragment")
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
//        activity!!.startActivityForResult(intent, REQUEST_CODE_SD_CARD_URI)
//    }

//    private fun requestSdCardUri() {
        //todo
//        val basePath = resourcesRepo.fileDataSource.getExtSdCardBaseFolder(pickedDir!!.path)
//        roomRepo.getSdCardUriByPath(basePath!!).observeOn(AndroidSchedulers.mainThread())
//            .subscribe({
//                it.uri = null
//                roomRepo.insertSdCardUri(it).observeOn(AndroidSchedulers.mainThread())
//                    .subscribe({ viewState.requestSdCardUri() }, {})
//            }, {
//                roomRepo.insertSdCardUri(SDCardUri(path = basePath))
//                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
//                        { viewState.requestSdCardUri() }, {})
//            })
//    }
}
package space.taran.arkbrowser.mvp.presenter

import space.taran.arkbrowser.mvp.model.repo.RootsRepo
import space.taran.arkbrowser.mvp.model.repo.ResourcesRepo
import space.taran.arkbrowser.mvp.model.repo.RoomRepo
import space.taran.arkbrowser.mvp.view.MainView
import space.taran.arkbrowser.navigation.Screens
import moxy.MvpPresenter
import ru.terrakok.cicerone.Router
import space.taran.arkbrowser.mvp.model.entity.Root
import javax.inject.Inject

class MainPresenter: MvpPresenter<MainView>() {
    @Inject
    lateinit var router: Router

    @Inject
    lateinit var roomRepo: RoomRepo

    @Inject
    lateinit var rootsRepo: RootsRepo

    @Inject
    lateinit var resourcesRepo: ResourcesRepo

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.init()
        viewState.requestPerms()
    }

    fun permsGranted() {
        loadSdCardUris()
    }

    fun permissionsDenied() {

    }

    private fun loadSdCardUris() {
        roomRepo.getSdCardUris().subscribe(
            { list ->
                resourcesRepo.documentDataSource.sdCardUris = list.toMutableList()
                loadAndSyncRoots()
            },
            {}
        )
    }

    private fun loadAndSyncRoots() {
        roomRepo.getAllRoots().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
                list.forEach { root ->
                    val storageVersion = resourcesRepo.readStorageVersion(root.storage)
                    if (storageVersion != ResourcesRepo.STORAGE_VERSION)
                        storageVersionDifferent(storageVersion, root)
                    rootsRepo.synchronizeRoot(root)
                }
                router.replaceScreen(Screens.RootScreen())
            },
            {}
        )
    }

    fun sdCardUriGranted(uri: String) {
        roomRepo.getSdCardUris().observeOn(AndroidSchedulers.mainThread()).subscribe(
            { list ->
               list.forEach {
                   if (it.uri == null) {
                       it.uri = uri
                       resourcesRepo.documentDataSource.sdCardUris.add(it)
                       roomRepo.insertSdCardUri(it).subscribe()
                   }
               }
            }, {}
        )
    }

    fun goToRootsScreen() {
        router.newRootScreen(Screens.RootScreen())
    }

    fun goToTagsScreen() {
        router.newRootScreen(Screens.TagsScreen(
            rootName = null,
            resources = rootsRepo.roots.values.flatMap { it.resources }))
    }

    fun goToExplorerScreen() {
        router.newRootScreen(Screens.ExplorerScreen())
    }

    fun backClicked() {
        router.exit()
    }

    private fun storageVersionDifferent(fileStorageVersion: Int, root: Root) {
        viewState.showToast("${root.storage.path} has a different version")
    }

}

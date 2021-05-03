package space.taran.arkbrowser.mvp.presenter.adapter

abstract class ReversibleItemGridPresenter<T>(
    initialFrame: List<T>, handler: (T) -> Unit)
        : IItemGridPresenter<T>(handler) {

    private val frames = mutableListOf(initialFrame)

    override fun items(): List<T> = frames.last()

    override fun backClicked() {
        frames.dropLast(0)
    }
}
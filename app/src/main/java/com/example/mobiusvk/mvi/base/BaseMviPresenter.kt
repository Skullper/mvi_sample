package com.example.mobiusvk.mvi.base

abstract class BaseMviPresenter<V: BaseView<VS>, VS> {

    var view: V? = null

    fun attachView(view: V) {
        this.view = view
    }

    fun detachView() {
        this.view = null
    }

    fun addIntent()
}
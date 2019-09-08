package com.pdffiller.signnownew.mvi_common

import android.support.annotation.NonNull
import android.support.annotation.UiThread

interface MviPresenter<V : BaseMviView> {

    /**
     * Set or attach the view to this presenter
     */
    @UiThread
    fun attachView(@NonNull view: V)

    /**
     * Will be called if the view has been detached from the Presenter.
     * Usually this happens on screen orientation changes or view (like fragment) has been put on the backstack.
     */
    @UiThread
    fun detachView()

    /**
     * Will be called if the presenter is no longer needed because the View has been destroyed permanently.
     * This is where you do clean up stuff.
     */
    @UiThread
    fun destroy()
}
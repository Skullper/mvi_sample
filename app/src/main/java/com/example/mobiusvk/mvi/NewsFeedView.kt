package com.example.mobiusvk.mvi

import com.example.mobiusvk.mvi.base.BaseView
import io.reactivex.Observable

interface NewsFeedView : BaseView {

    fun loadNewsIntent(): Observable<Boolean>

    fun render(state: NewsFeedViewState)
}
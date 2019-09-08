package com.example.mobiusvk.mvi

import com.pdffiller.signnownew.mvi_common.BaseMviView
import io.reactivex.Observable

interface NewsFeedView : BaseMviView {

    fun loadNewsIntent(): Observable<Boolean>

    fun render(state: NewsFeedViewState)
}
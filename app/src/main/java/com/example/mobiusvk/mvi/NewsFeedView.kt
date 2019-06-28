package com.example.mobiusvk.mvi

import com.example.mobiusvk.mvi.base.BaseView

interface NewsFeedView: BaseView<NewsFeedViewState> {

    fun loadNewsIntent()
}
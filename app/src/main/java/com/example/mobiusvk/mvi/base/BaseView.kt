package com.example.mobiusvk.mvi.base

interface BaseView<VS> {

    fun render(viewState: VS)
}
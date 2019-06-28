package com.example.mobiusvk.mvi

import com.example.mobiusvk.mvi.base.BaseMviPresenter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class NewsFeedPresenter: BaseMviPresenter<NewsFeedView, NewsFeedViewState>() {

    private val dataProvider = DataProvider()

    fun loadStoredNews() {
        var disposable: Disposable? = null
        disposable = dataProvider.getStoredData()
            .toObservable()
            .map { NewsFeedViewState.StoredNews(it) }
            .cast(NewsFeedViewState::class.java)
            .onErrorReturn { error -> NewsFeedViewState.ErrorWithNewsLoading(error) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ viewState ->
                view?.render(viewState)
            }, {
                it.printStackTrace()
            }, {
                disposable?.dispose()
            })
    }

    fun loadNews() {
        var disposable: Disposable? = null
        disposable = dataProvider.getData()
            .toObservable()
            .map { NewsFeedViewState.RetrievedNews(it) }
            .cast(NewsFeedViewState::class.java)
            .startWith (NewsFeedViewState.LoadingNews)
            .onErrorReturn { error -> NewsFeedViewState.ErrorWithNewsLoading(error) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ viewState ->
                view?.render(viewState)
            }, {
                it.printStackTrace()
            }, {
                disposable?.dispose()
            })
    }

}
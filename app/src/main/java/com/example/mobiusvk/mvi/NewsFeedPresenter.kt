package com.example.mobiusvk.mvi

import com.example.mobiusvk.mvi.base.BaseMviPresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class NewsFeedPresenter : BaseMviPresenter<NewsFeedView, NewsFeedViewState>() {

    private val dataProvider = DataProvider()

    override fun bindIntents() {
        val result = intent(object : ViewIntentBinder<NewsFeedView, Boolean> {
            override fun bind(view: NewsFeedView): Observable<Boolean> {
                return view.loadNewsIntent()
            }
        })
            .flatMap { dataProvider.getData().toObservable() }
            .map { NewsFeedViewState.RetrievedNews(it) }
            .cast(NewsFeedViewState::class.java)
            .startWith(NewsFeedViewState.LoadingNews)
            .onErrorReturn { error -> NewsFeedViewState.ErrorWithNewsLoading(error) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        subscribeViewState(result, object : ViewStateConsumer<NewsFeedView, NewsFeedViewState>{
            override fun accept(view: NewsFeedView, viewState: NewsFeedViewState) {
                view.render(viewState)
            }
        })
    }

}
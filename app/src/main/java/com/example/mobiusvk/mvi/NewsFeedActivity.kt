package com.example.mobiusvk.mvi

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.example.mobiusvk.R
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_news_feed.*

class NewsFeedActivity: AppCompatActivity(), NewsFeedView {

    private val presenter = NewsFeedPresenter()
    private val adapter = FeedAdapter()

    override fun render(state: NewsFeedViewState) {
        if(state is NewsFeedViewState.LoadingNews) {
            pb_news_feed.visibility = View.VISIBLE
        } else {
            pb_news_feed.visibility = View.GONE
        }

        when(state) {
            is NewsFeedViewState.ErrorWithNewsLoading -> Log.e("TAGA", "Error: ${state.error.message}")
            is NewsFeedViewState.StoredNews -> adapter.items = state.list
            is NewsFeedViewState.RetrievedNews -> adapter.items = state.list
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_feed)
        initFeed()
        presenter.attachView(this)
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun loadNewsIntent(): Observable<Boolean> {
        return Observable.just(true)
    }

    private fun initFeed() {
        rv_news.apply {
            layoutManager = LinearLayoutManager(this@NewsFeedActivity)
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = this@NewsFeedActivity.adapter
        }
    }
}
package com.example.mobiusvk.mvi

sealed class NewsFeedViewState {
    object LoadingNews: NewsFeedViewState()
    data class StoredNews(val list: List<FeedItem>): NewsFeedViewState()
    data class RetrievedNews(val list: List<FeedItem>): NewsFeedViewState()
    data class ErrorWithNewsLoading(val error: Throwable): NewsFeedViewState()
}
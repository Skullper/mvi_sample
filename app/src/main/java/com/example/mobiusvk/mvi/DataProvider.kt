package com.example.mobiusvk.mvi

import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

class DataProvider {

    fun getData(): Single<List<FeedItem>> {
        return Observable.fromIterable(('A'..'Z'))
            .delay(2000, TimeUnit.MILLISECONDS)
            .map { FeedItem(it.toString(), "$it description") }
            .toList()
    }

    fun getStoredData(): Single<List<FeedItem>> {
        return Observable.fromIterable(('A'..'E'))
            .map { FeedItem(it.toString(), "$it description") }
            .toList()
    }
}
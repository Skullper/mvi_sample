package com.example.mobiusvk.mvi.base

import android.support.annotation.MainThread
import io.reactivex.Observable
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.reactivex.subjects.UnicastSubject


abstract class BaseMviPresenter<V: BaseView, VS> {

    private var viewStateBehaviorSubject: BehaviorSubject<VS>

    init {
        viewStateBehaviorSubject = BehaviorSubject.create()
    }

    var view: V? = null
    private val intentList = mutableListOf<Observable<Any>>()

    fun attachView(view: V) {
        this.view = view
        bindIntents()

        if (viewStateConsumer != null) {
            subscribeViewStateConsumerActually(view)
        }

        val intentsSize = intentRelaysBinders.size
        for (i in 0 until intentsSize) {
            val intentRelayBinderPair = intentRelaysBinders[i]
            bindIntentActually<Any>(view, intentRelayBinderPair as IntentRelayBinderPair<*>)
        }
    }

    fun detachView() {
        this.view = null
    }

    protected interface ViewIntentBinder<V : BaseView, I> {
        @NonNull
        fun bind(@NonNull view: V): Observable<I>
    }

    private inner class IntentRelayBinderPair<I>(
        val intentRelaySubject: Subject<I>,
        val intentBinder: ViewIntentBinder<V, I>
    )

    private val intentRelaysBinders = arrayListOf<Any>()

    @MainThread
    protected fun <I> intent(binder: ViewIntentBinder<V, I>): Observable<I> {
        val intentRelay = UnicastSubject.create<I>()
        intentRelaysBinders.add(IntentRelayBinderPair<I>(intentRelay, binder))
        return intentRelay
    }

    private var viewStateConsumer: ViewStateConsumer<V, VS>? = null

    private var viewStateDisposable: Disposable? = null

    private var viewRelayConsumerDisposable: Disposable? = null

    private var intentDisposables: CompositeDisposable? = null

    protected interface ViewStateConsumer<V : BaseView, VS> {
        fun accept(view: V, viewState: VS)
    }

    internal class DisposableViewStateObserver<VS>(private val subject: BehaviorSubject<VS>) :
        DisposableObserver<VS>() {

        override fun onNext(value: VS) {
            subject.onNext(value)
        }

        override fun onError(e: Throwable) {
            throw IllegalStateException(
                "ViewState observable must not reach error state - onError()", e
            )
        }

        override fun onComplete() {
            // ViewState observable never completes so ignore any complete event
        }
    }

    @MainThread
    protected abstract fun bindIntents()

    @MainThread
    protected fun subscribeViewState(
        viewStateObservable: Observable<VS>,
        consumer: ViewStateConsumer<V, VS>
    ) {
//        if (subscribeViewStateMethodCalled) {
//            throw IllegalStateException(
//                "subscribeViewState() method is only allowed to be called once"
//            )
//        }
//        subscribeViewStateMethodCalled = true

//        if (viewStateObservable == null) {
//            throw NullPointerException("ViewState Observable is null")
//        }
//
//        if (consumer == null) {
//            throw NullPointerException("ViewStateBinder is null")
//        }

        this.viewStateConsumer = consumer

        viewStateDisposable = viewStateObservable.subscribeWith(
            DisposableViewStateObserver(viewStateBehaviorSubject)
        )
    }

    @MainThread
    private fun subscribeViewStateConsumerActually(view: V) {

        if (view == null) {
            throw NullPointerException("View is null")
        }

//        if (viewStateConsumer == null) {
//            throw NullPointerException(ViewStateConsumer<*, *>::class.java.simpleName + " is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues")
//        }

        viewRelayConsumerDisposable = viewStateBehaviorSubject.subscribe { vs -> viewStateConsumer!!.accept(view, vs) }
    }

    @MainThread
    private fun <I> bindIntentActually(
        view: V,
        relayBinderPair: IntentRelayBinderPair<*>
    ): Observable<I> {

        if (view == null) {
            throw NullPointerException(
                "View is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"
            )
        }

        if (relayBinderPair == null) {
            throw NullPointerException(
                "IntentRelayBinderPair is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"
            )
        }

        val intentRelay = relayBinderPair.intentRelaySubject as Subject<I> ?: throw NullPointerException(
            "IntentRelay from binderPair is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"
        )

        val intentBinder = relayBinderPair.intentBinder as ViewIntentBinder<V, I> ?: throw NullPointerException(
            ViewIntentBinder::class.java.simpleName + " is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"
        )
        val intent = intentBinder.bind(view) ?: throw NullPointerException(
            "Intent Observable returned from Binder $intentBinder is null"
        )

        if (intentDisposables == null) {
            intentDisposables = CompositeDisposable()
        }

        intentDisposables?.add(intent.subscribeWith(DisposableIntentObserver<I>(intentRelay)))
        return intentRelay
    }

    internal class DisposableIntentObserver<I>(private val subject: Subject<I>) : DisposableObserver<I>() {

        override fun onNext(value: I) {
            subject.onNext(value)
        }

        override fun onError(e: Throwable) {
            throw IllegalStateException("View intents must not throw errors", e)
        }

        override fun onComplete() {
            subject.onComplete()
        }
    }
}
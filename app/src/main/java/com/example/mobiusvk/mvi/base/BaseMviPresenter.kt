package com.pdffiller.signnownew.mvi_common

import android.support.annotation.MainThread
import io.reactivex.Observable
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.reactivex.subjects.UnicastSubject

abstract class BaseMviPresenter<V : BaseMviView, VS> : MviPresenter<V> {

    /**
     * The binder is responsible for binding a single view intent.
     * Typically, you use that in {@link #bindIntents()} in combination with the {@link
     * #intent(ViewIntentBinder)} function like this:
     * <pre><code>
     *   Observable<Boolean> loadIntent = intent(new ViewIntentBinder() {
     *      @Override
     *      public Observable<Boolean> bind(MyView view){
     *         return view.loadIntent();
     *      }
     *   }
     * </code></pre>
     *
     * @param <V> The View type
     * @param <I> The type of the Intent
     */
    protected interface ViewIntentBinder<V : BaseMviView, I> {
        @NonNull
        fun bind(@NonNull view: V): Observable<I>?
    }

    /**
     * This "binder" is responsible for binding the view state to the currently attached view.
     * This typically "renders" the view.
     * <p>
     * Typically, this is used in {@link #bindIntents()} with
     * {@link #subscribeViewState(Observable, ViewStateConsumer)} like this:
     * <pre><code>
     *   Observable<MyViewState> viewState =  ... ;
     *   subscribeViewStateConsumerActually(viewState, new ViewStateConsumer() {
     *      @Override
     *      public void accept(MyView view, MyViewState viewState){
     *         view.render(viewState);
     *      }
     *   }
     * </code></pre>
     *
     * @param <V>  The view type
     * @param <VS> The ViewState type
     */
    protected interface ViewStateConsumer<V : BaseMviView, VS> {
        fun accept(view: V, viewState: VS)
    }

    /**
     * A simple class that holds a pair of the Intent relay and the binder to bind the actual Intent
     * Observable.
     *
     * @param <I> The Intent type
     */
    private inner class IntentRelayBinderPair<I>(
            val intentRelaySubject: Subject<I>,
            val intentBinder: ViewIntentBinder<V, I>
    )

    /**
     * This relay is the bridge to the viewState (UI). Whenever the viewState gets re-attached, the
     * latest state will be re-emitted.
     */
    private var viewStateBehaviorSubject: BehaviorSubject<VS>

    /**
     * We only allow to call [.subscribeViewState] method once
     */
    private var subscribeViewStateMethodCalled = false

    /**
     * List of internal relays, bridging the gap between intents coming from the viewState (will be
     * unsubscribed temporarily when viewState is detached i.e. during config changes)
     */
    private val intentRelaysBinders = arrayListOf<Any>()

    /**
     * Composite Disposables holding subscriptions to all intents observable offered by the viewState.
     */
    private var intentDisposables: CompositeDisposable? = null

    /**
     * Disposable to unsubscribe from the viewState when the viewState is detached (i.e. during screen
     * orientation changes)
     */
    private var viewRelayConsumerDisposable: Disposable? = null

    /**
     * Disposable between the viewState observable returned from {@link #intent(ViewIntentBinder)}
     * and {@link #viewStateBehaviorSubject}
     */
    private var viewStateDisposable: Disposable? = null

    /**
     * Used to determine whether or not a View has been attached for the first time.
     * This is used to determine whether or not the intents should be bound via {@link
     * #bindIntents()} or rebound internally.
     */
    private var viewAttachedFirstTime = true

    /**
     * This binder is used to subscribe the view's render method to render the ViewState in the view.
     */
    private var viewStateConsumer: ViewStateConsumer<V, VS>? = null

    /**
     * Creates a new Presenter without an initial view state
     */
    constructor() {
        viewStateBehaviorSubject = BehaviorSubject.create()
        reset()
    }

    /**
     * Creates a new Presenter with the initial view state
     *
     * @param initialViewState initial view state (must be not null)
     */
    constructor(@NonNull initialViewState: VS?) {
        if (initialViewState == null) {
            throw NullPointerException("Initial ViewState == null")
        }

        viewStateBehaviorSubject = BehaviorSubject.createDefault(initialViewState)
        reset()
    }

    /**
     * Gets the view state observable.
     *
     *
     * Most likely you will use this method for unit testing your presenter.
     *
     *
     *
     *
     *
     * In some very rare case it could be useful to provide other components, such as other presenters,
     * access to the state. This observable contains the same value as the one from [ ][.subscribeViewState] which is also used to render the view.
     * In other words, this observable also represents the state of the View, so you could subscribe
     * via this observable to the view's state.
     *
     *
     * @return Observable
     */
    protected fun getViewStateObservable(): Observable<VS> {
        return viewStateBehaviorSubject
    }

    @Suppress("UNCHECKED_CAST")
    override fun attachView(view: V) {
        if (viewAttachedFirstTime) bindIntents()

        if (viewStateConsumer != null) {
            subscribeViewStateConsumerActually(view)
        }

        val intentsSize = intentRelaysBinders.size
        for (i in 0 until intentsSize) {
            val intentRelayBinderPair = intentRelaysBinders[i]
            bindIntentActually<Any>(view, intentRelayBinderPair as IntentRelayBinderPair<*>)
        }

        viewAttachedFirstTime = false
    }

    override fun detachView() {
        // Cancel subscription from View to viewState Relay
        viewRelayConsumerDisposable?.dispose()
        viewRelayConsumerDisposable = null

        // Cancel subscriptions from view intents to intent Relays
        intentDisposables?.dispose()
        intentDisposables = null
    }

    override fun destroy() {
        viewStateDisposable?.dispose()

        unbindIntents()
        reset()
        // TODO should we re-emit the initial state? What if no initial state has been set?
        // TODO should we rather throw an exception if presenter is reused after view has been detached permanently
    }

    /**
     * This is called when the View has been detached permanently (view is destroyed permanently)
     * to reset the internal state of this Presenter to be ready for being reused (even though
     * reusing presenters after their view has been destroy is BAD)
     */
    private fun reset() {
        viewAttachedFirstTime = true
        intentRelaysBinders.clear()
        subscribeViewStateMethodCalled = false
    }

    /**
     * This method subscribes the Observable emitting {@code ViewState} over time to the passed
     * consumer.
     * <b>Only invoke this method once! Typically, in {@link #bindIntents()}</b>
     * <p>
     * Internally, Mosby will hold some relays to ensure that no items emitted from the ViewState
     * Observable will be lost while viewState is not attached nor that the subscriptions to
     * viewState intents will cause memory leaks while viewState detached.
     * </p>
     * <p>
     * Typically, this method is used in {@link #bindIntents()}  like this:
     * <pre><code>
     *   Observable<MyViewState> viewState =  ... ;
     *   subscribeViewStateConsumerActually(viewState, new ViewStateConsumer() {
     *      @Override
     *      public void accept(MyView view, MyViewState viewState){
     *         view.render(viewState);
     *      }
     *   }
     * </code></pre>
     *
     * @param viewStateObservable The Observable emitting new ViewState. Typically, an intent {@link
     *                            #intent(ViewIntentBinder)} causes the underlying business logic to do a change and eventually
     *                            create a new ViewState.
     * @param consumer            {@link ViewStateConsumer} The consumer that will update ("render") the view.
     */
    @MainThread
    protected fun subscribeViewState(
            viewStateObservable: Observable<VS>?,
            consumer: ViewStateConsumer<V, VS>?
    ) {
        check(!subscribeViewStateMethodCalled) { "subscribeViewState() method is only allowed to be called once" }
        subscribeViewStateMethodCalled = true

        check(viewStateObservable != null) { throw NullPointerException("ViewState Observable is null") }

        check(consumer != null) { throw NullPointerException("ViewStateBinder is null") }

        this.viewStateConsumer = consumer

        viewStateDisposable = viewStateObservable.subscribeWith(
                DisposableViewStateObserver(viewStateBehaviorSubject)
        )
    }

    /**
     * Actually subscribes the view as consumer to the internally view relay.
     *
     * @param view The mvp view
     */
    @MainThread
    private fun subscribeViewStateConsumerActually(view: V?) {

        check(view != null) { throw NullPointerException("View is null") }

        check(viewStateConsumer != null) {
            throw NullPointerException("View state consumer is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues")
        }

        viewRelayConsumerDisposable = viewStateBehaviorSubject.subscribe { vs -> viewStateConsumer!!.accept(view, vs) }
    }

    /**
     * This method is called once the view is attached to this presenter for the very first time.
     * For instance, it will not be called again during screen orientation changes when the view will be
     * detached temporarily.
     * <p>
     * <p>
     * The counter part of this method is {@link #unbindIntents()}.
     * This {@link #bindIntents()} and {@link #unbindIntents()} are kind of representing the
     * lifecycle of this Presenter.
     * {@link #bindIntents()} is called the first time the view is attached
     * and {@link #unbindIntents()} is called once the view is detached permanently because it has
     * been destroyed and hence this presenter is not needed anymore and will also be destroyed
     * afterwards
     * </p>
     */
    @MainThread
    protected abstract fun bindIntents()

    /**
     * This method will be called once the view has been detached permanently and hence the presenter
     * will be "destroyed" too. This is the correct time for doing some cleanup like unsubscribe from
     * RxSubscriptions, etc.
     *
     *
     *
     *
     * The counter part of this method is [.bindIntents] ()}.
     * This [.bindIntents] and [.unbindIntents] are kind of representing the
     * lifecycle of this Presenter.
     * [.bindIntents] is called the first time the view is attached
     * and [.unbindIntents] is called once the view is detached permanently because it has
     * been destroyed and hence this presenter is not needed anymore and will also be destroyed
     * afterwards
     *
     */
    protected fun unbindIntents() {}

    /**
     * This method creates a decorator around the original view's "intent". This method ensures that
     * no memory leak by using a {@link ViewIntentBinder} is caused by the subscription to the original
     * view's intent when the view gets detached.
     * <p>
     * Typically, this method is used in {@link #bindIntents()} like this:
     * <pre><code>
     *   Observable<Boolean> loadIntent = intent(new ViewIntentBinder() {
     *      @Override
     *      public Observable<Boolean> bind(MyView view){
     *         return view.loadIntent();
     *      }
     *   }
     * </code></pre>
     *
     * @param binder The {@link ViewIntentBinder} from where the the real view's intent will be
     *               bound
     * @param <I>    The type of the intent
     * @return The decorated intent Observable emitting the intent
     */
    @MainThread
    protected fun <I> intent(binder: ViewIntentBinder<V, I>): Observable<I> {
        val intentRelay = UnicastSubject.create<I>()
        intentRelaysBinders.add(IntentRelayBinderPair<I>(intentRelay, binder))
        return intentRelay
    }

    @Suppress("UNCHECKED_CAST")
    @MainThread
    private fun <I> bindIntentActually(
            view: V?,
            relayBinderPair: IntentRelayBinderPair<*>?
    ): Observable<I> {

        check(view != null) {
            throw NullPointerException("View is null. Check your view initialize method")
        }

        check(relayBinderPair != null) {
            throw NullPointerException(
                    "IntentRelayBinderPair is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"
            )
        }

        val intentRelay = relayBinderPair.intentRelaySubject as? Subject<I>
                ?: throw NullPointerException(
                        "IntentRelay from binderPair is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues")

        val intentBinder = relayBinderPair.intentBinder as? ViewIntentBinder<V, I>
                ?: throw NullPointerException((ViewIntentBinder::class.java.simpleName + " is null. This is a Mosby internal bug. Please file an issue at https://github.com/sockeqwe/mosby/issues"))

        val intent = intentBinder.bind(view)
                ?: throw NullPointerException(
                        "Intent Observable returned from Binder $intentBinder is null")
        if (intentDisposables == null) {
            intentDisposables = CompositeDisposable()
        }

        intentDisposables?.add(intent.subscribeWith(DisposableIntentObserver(intentRelay)))
        return intentRelay
    }

    /**
     * Just a simple {@link DisposableObserver} that is used to cancel subscriptions from view's
     * intent to the internal relays
     */
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

    /**
     * Just a simple {@link DisposableObserver} that is used to cancel subscriptions from view's
     * state to the internal relays
     */
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

}
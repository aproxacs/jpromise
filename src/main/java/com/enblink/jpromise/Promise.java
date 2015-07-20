package com.enblink.jpromise;


import com.enblink.jpromise.callbacks.*;

/**
 * Promise interface following to deferred/promise pattern of node.js.
 * Promise does not have writable interfaces. You may be confused if you already know about
 * the concept of future and promise from the wiki.(https://en.wikipedia.org/wiki/Futures_and_promises)
 * It says 'a future is a read-only placeholder view of a variable, while a promise is a writable'
 *
 * However, in node.js, promise does not have writable interfaces. It is similar to future from the wiki.
 * And deferred object works like a promise from the wiki.
 *
 * We follows the concept used by node.js.
 *
 * @param <T> the type of result object.
 */
public interface Promise<T>
{
	enum State
	{
		Pending,
		Resolved,
		Rejected
	}

	/**
	 * Returns a new promise for pipelining the result of promise. Let's call the new promise a piplelining promise.
	 * PipeCallback is executed when the promise is resolved. Callback returns another promise. Let's call it a returned promise.
	 * Pipelining promise is resolved when the returned promise is resolved.
	 * The resolved value of pipelining promise is the result of returned promise.
	 *
	 * Pipelining promise is rejected when this promise or returned promise is rejected.
	 * When the exception occurs during callback, pipelining promise is rejected.
	 *
	 * Example :
	 *
	 * <code>
	 * 	DeferredObject<Integer> deferred = new DeferredObject<>();
	 * 	promise.then(result ->
	 * 	{
	 * 		DeferredObject<String> deferred = new DeferredObject<String>();
	 * 	    new Thread(() ->
	 * 	    {
	 * 	    	// ...
	 * 	    	deferred.resolve("then result");
	 * 	    }).start();
	 *
	 * 	    return deferred.promise();
	 * 	}).done(result -> {
	 * 	  System.out.println(result); // result is 'then result'
	 * 	});
	 *
	 * 	deferred.resolve(3);
	 * </code>
	 *
	 * @param callback callback to be executed when the promise is resolved.
	 * @param <R> the type of the returned value of the callback.
	 * @return new promise of R
	 */
	<R> Promise<R> then(PipeCallback<T, R> callback);

	/**
	 * then method also accepts DoneCallback. Done callback does not returns anything.
	 * The main difference between then method and done method is then method returns a new pipelining promise
	 * while done method returns self. Exceptions during DoneCallback are handled, so cause the promise rejected.
	 *
	 * If you want to keep pipelining and for exceptions to be handled in the process of the pipelining, this method is
	 * the option.
	 *
	 * Example :
	 *
	 * <code>
	 * 	DeferredObject<Integer> deferred = new DeferredObject<>();
	 * 	promise.then(result ->
	 * 	{
	 * 		boolean error = doSomeThing();
	 * 		if(error) throw new RuntimeException();
	 *
	 * 	}).then(...).fail(error ->
	 * 	{
	 * 		// exceptions will be handled in here
	 * 	});
	 *
	 * 	deferred.resolve(3);
	 * </code>
	 *
	 * @param callback
	 * @return
	 */
	Promise<Void> then(DoneCallback<T> callback);

	/**
	 * Returns a new promise for pipelining the result of promise. Let's call the new promise piplelining promise.
	 * Pipelining promise is rejected when this promise is rejected.
	 * When the exception occurs during callback, pipelining promise is rejected.
	 * Pipelining promise is resolved when this promise is resolved.
	 * The resolved value of pipelining promise is the returned value of the map callback.
	 *
	 * Example :
	 *
	 * <code>
	 * 	DeferredObject<Integer> deferred = new DeferredObject<>();
	 * 	promise.map(result -> result.toString());
	 * 	deferred.resolve(3);
	 * </code>
	 *
	 * @param callback map callback to be executed when the promise is resolved.
	 * @param <R> the type of the returned value of the map callback.
	 * @return new promise of R
	 */
	<R> Promise<R> map(MapCallback<T, R> callback);

	/**
	 * Adds 'progress callback' and returns this promise.
	 * 'progress callback' will be called when the promise is progressed.
	 * If the promise is resolved already, 'progress callback' will be executed immediately with 100.
	 * The exception that occurs during callback will be ignored.
	 *
	 * @param callback progress callback
	 * @return this promise
	 */
	Promise<T> progress(ProgressCallback callback);

	/**
	 * Adds 'done callback' and returns this promise.
	 * 'done callback' will be called when the promise is resolved.
	 * If the promise is resolved already, 'done callback' will be executed immediately.
	 *
	 * @param callback done callback.
	 * @return this promise
	 */
	Promise<T> done(DoneCallback<T> callback);


	/**
	 * Adds 'fail callback', and returns this promise.
	 * 'fail callback' will be called when the promise is rejected.
	 * If the promise is already rejected, 'fail callback' will be executed immediately.
	 *
	 * @param callback fail callback
	 * @return this promise
	 */
	Promise<T> fail(FailCallback callback);


	/**
	 * Adds 'always callback', and returns this promise.
	 * 'always callback' is called whether the promise is resolved or rejected
	 *
	 * @param callback alwyas callback
	 * @return this promise
	 */
	Promise<T> always(AlwaysCallback callback);


	/**
	 *
	 * @return true if rejected.
	 * 		   false otherwise.
	 */
	boolean isRejected();

	/**
	 *
	 * @return true if resolved
	 * 			false otherwise.
	 */
	boolean isResolved();

	/**
	 *
	 * @return State of the promise
	 */
	State getState();


	/**
	 *
	 * @return the result of the promise when resolved.
	 */
	T getResult();

	/**
	 *
	 * @return the error of the promise when rejected.
	 */
	Throwable getCause();

}

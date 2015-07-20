package com.enblink.jpromise;

import com.enblink.jpromise.callbacks.*;
import io.netty.util.concurrent.*;

import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.Throwable;
import java.util.ArrayList;
import java.util.List;

/**
 * DeferredObject is the writable promise that implements Promise interface using DefaultPromise of netty.
 *
 * The main reason why we use netty's promise is because we can execute the callback in the thread we want using netty's
 * executor.
 *
 * If you are using netty, ChannelHandlerContext provides your current executor. Or you can use GlobalEventExecutor.
 * If you do not specify any executor, the default one is ImmediateEventExecutor, which callback is executed in the thread
 * that the callback is executed.
 *
 * It also provides all method, which returns new promise that will be resolved when all promises are resolved.
 *
 * Examples :
 *
 * <code>
 *  public Promise<Object> doPromise()
 *  {
 * 		DeferredObject<Object> deferred = new DeferredObject<>(channelContext.executor());
 *
 * 		try {
 * 			Object result = doAsyncJob((Object result) -> {
 * 				deferred.resolve(result);
 * 			});
 * 		}
 * 		catch(Throwable cause)
 * 		{
 * 	    	deferred.rejected(cause);
 * 		}
 *
 * 		return deferred.promise();
 *  }
 * </code>
 *
 * <code>
 *     promise = doPromise();
 *     promise.then(object -> ...).done(object ->... ).fail(cause -> ...);
 * </code>
 *
 * @param <T> see Promise
 */
public class DeferredObject<T> implements com.enblink.jpromise.Promise<T>
{
	private DefaultPromise<T> nettyPromise;
	private EventExecutor executor;
	private State state;

	private List<ProgressCallback> progressCallbacks = new ArrayList<>();

	/**
	 * returns MultipleDeferredObject promise. This promise is resolved when all the promises are rejoved.
	 * If one of the promise fails, it is not resolved, but rejected.
	 *
	 * @param promises promises
	 * @return MultipleDeferredObject object.
	 */
	public static com.enblink.jpromise.Promise<Object[]> all(com.enblink.jpromise.Promise... promises)
	{
		if (promises == null || promises.length == 0)
			throw new IllegalArgumentException("Arguments is null or its length is empty");

		return new MultipleDeferredObject(promises).promise();
	}

	public static com.enblink.jpromise.Promise<Object[]> all(List<com.enblink.jpromise.Promise> promises)
	{
		com.enblink.jpromise.Promise[] array = promises.toArray(new com.enblink.jpromise.Promise[promises.size()]);
		return all(array);
	}


	/**
	 * Creates Deferred Object using ImmediateEventExecutor
	 */
	public DeferredObject()
	{
		this(ImmediateEventExecutor.INSTANCE);
	}


	/**
	 * Creates Deferred Object using given executor
	 * For example, you can use GlobalEventExecutor like following.
	 *
	 * <code>
	 * 	DeferredObject<Object> deferred = new DeferredObject(GlobalEventExecutor.INSTANCE);
	 * </code>
	 *
	 * @param executor EventExecutor which callback will be executed.
	 */
	public DeferredObject(EventExecutor executor)
	{
		this.executor = executor;
		nettyPromise = new DefaultPromise<>(executor);
		this.state = State.Pending;
	}


	//region - Promise inteface overrides
	@Override
	public boolean isRejected()
	{
		return state == State.Rejected;
	}

	@Override
	public boolean isResolved()
	{
		return state == State.Resolved;
	}

	public boolean isPending()
	{
		return state == State.Pending;
	}

	@Override
	public State getState()
	{
		return state;
	}


	@Override
	public T getResult()
	{
		return nettyPromise.getNow();
	}

	@Override
	public Throwable getCause()
	{
		return nettyPromise.cause();
	}

	@Override
	public <R> com.enblink.jpromise.Promise<R> then(PipeCallback<T, R> callback)
	{
		DeferredObject<R> deferred = new DeferredObject<>(executor);

		nettyPromise.addListener(new NettyFutureListener<>(deferred, callback));

		return deferred.promise();
	}


	@Override
	public com.enblink.jpromise.Promise<Void> then(DoneCallback<T> callback)
	{
		DeferredObject<Void> deferred = new DeferredObject<>(executor);

		nettyPromise.addListener(new NettyFutureListener<>(deferred, callback));

		return deferred.promise();

	}

	@Override
	public <R> com.enblink.jpromise.Promise<R> map(MapCallback<T, R> callback)
	{
		DeferredObject<R> deferred = new DeferredObject<>(executor);

		nettyPromise.addListener(new NettyFutureListener<>(deferred, callback));

		return deferred.promise();
	}

	@Override
	public com.enblink.jpromise.Promise<T> progress(ProgressCallback callback)
	{
		if(callback == null) return this;

		if(isResolved()) {
			runProgressCallback(callback, 100);
		}
		else if(isPending()){
			progressCallbacks.add(callback);
		}

		return this;
	}

	@Override
	public com.enblink.jpromise.Promise<T> done(DoneCallback<T> callback)
	{
		if(callback == null) return this;

		nettyPromise.addListener(future ->
		{
			if(future.isSuccess())
			{
				T result = (T)future.get();
				callback.onDone(result);
			}
		});

		return this;
	}

	@Override
	public com.enblink.jpromise.Promise<T> fail(final FailCallback callback)
	{
		if(callback == null) return this;

		nettyPromise.addListener(future ->
		{
			if(!future.isSuccess())
			{
				callback.onFail(future.cause());
			}
		});

		return this;
	}

	@Override
	public Promise<T> always(AlwaysCallback callback)
	{
		if(callback == null) return this;

		nettyPromise.addListener(future ->
		{
			callback.onDone();
		});

		return this;
	}

	//endregion


	//region - writable intefaces
	/**
	 * Sets the progress
	 *
	 * @param progress the number of progress(0 to 100)
	 * @return this promise.
	 */
	public com.enblink.jpromise.Promise<T> setProgress(int progress)
	{
		if(progress > 100) progress = 100;
		if(progress < 0) progress = 0;

		int value = progress;
		for(ProgressCallback callback:progressCallbacks)
		{
			runProgressCallback(callback, value);
		}

		return this;
	}


	/**
	 * Resolves this promise with null value.
	 *
	 * @return this promise
	 */
	public com.enblink.jpromise.Promise<T> resolve()
	{
		return resolve(null);
	}

	/**
	 * Resolves this promise. As a result, calls registered done callbacks.
	 *
	 * @param result resolved object.
	 * @return this promise.
	 */
	public com.enblink.jpromise.Promise<T> resolve(T result)
	{
		// allows only pending -> resolved
		if(state != State.Pending) return this;

		state = State.Resolved;
		nettyPromise.setSuccess(result);

		return this;
	}

	/**
	 * Rejects this promise, As a result, calls registered fail callbacks.
	 *
	 * @param reason reason why it is rejected.
	 * @return this promise.
	 */
	public com.enblink.jpromise.Promise<T> reject(Throwable reason)
	{
		if(isRejected()) return this;

		state = State.Rejected;
		nettyPromise.setFailure(reason);

		return this;
	}
	//endregion

	/**
	 * Returns this deferred object, but it is not writable.
	 *
	 * @return this promise
	 */
	public com.enblink.jpromise.Promise<T> promise()
	{
		return this;
	}




	//region - private methods
	private void runProgressCallback(ProgressCallback callback, int progress)
	{
		try {
			callback.onProgress(progress);
		}
		catch(Throwable error)
		{
			// do nothing
		}
	}
	//endregion




	//region
	private class NettyFutureListener<FT, R> implements GenericFutureListener<Future<? super FT>>
	{

		DeferredObject<R> deferred;
		PipeCallback<FT, R> pipeCallback;
		MapCallback<FT, R> mapCallback;
		DoneCallback<FT> doneCallback;


		private NettyFutureListener(DeferredObject<R> deferred, PipeCallback<FT, R> callback)
		{
			this.deferred = deferred;
			this.pipeCallback = callback;
		}

		private NettyFutureListener(DeferredObject<R> deferred, MapCallback<FT, R> callback)
		{
			this.deferred = deferred;
			this.mapCallback = callback;
		}

		private NettyFutureListener(DeferredObject<R> deferred, DoneCallback<FT> callback)
		{
			this.deferred = deferred;
			this.doneCallback = callback;
		}

		@Override
		public void operationComplete(Future<? super FT> future) throws Exception
		{
			if (!future.isSuccess() && deferred.isPending())
			{
				deferred.reject(future.cause());
				return;
			}

			try
			{
				FT result = (FT)future.get();
				executeCallback(result);
			}
			// when failed while executing callback, make the promise rejected.
			catch (Throwable cause)
			{
				deferred.reject(cause);
			}

		}

		private void executeCallback(FT result) throws Throwable
		{
			if(pipeCallback != null)
			{
				com.enblink.jpromise.Promise<R> promise = pipeCallback.onDone(result);
				if(promise == null) {
					deferred.resolve();
				}
				else {
					pipePromise(promise);
				}

			}
			else if(doneCallback != null)
			{
				doneCallback.onDone(result);
				deferred.resolve();
			}
			else if(mapCallback != null)
			{
				R obj = mapCallback.onDone(result);
				deferred.resolve(obj);
			}
		}

		/**
		 * pipes the result of the promise to the deferred.
		 */
		private void pipePromise(com.enblink.jpromise.Promise<R> promise)
		{
			// deferred will be resolved when the promise is resolved.
			promise.done(deferred::resolve);

			// deferred will be rejected when the promise is rejected.
			promise.fail(deferred::reject);
		}
	}
	//endregion


}


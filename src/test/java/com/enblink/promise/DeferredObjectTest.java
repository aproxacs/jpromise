package com.enblink.promise;


import com.enblink.jpromise.DeferredObject;
import com.enblink.jpromise.Promise;
import com.enblink.jpromise.callbacks.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DeferredObjectTest
{
	DeferredObject<String> deferred;

	boolean isDone = false;
	boolean isFailed = false;
	boolean isAlways = false;
	boolean isMapped = false;
	boolean isPiped = false;
	boolean isFailedFailed = false;
	Integer progress = 0;

	DoneCallback<String> doneCallback = result -> isDone = true;
	FailCallback failCallback = result -> isFailed = true;
	ProgressCallback progressCallback = value -> progress = value;
	AlwaysCallback alwaysCallback = () -> isAlways = true;
	MapCallback<String, String> mapCallback = result ->
	{
		isMapped = true;
		return "map result";
	};

	PipeCallback<String, String> pipeCallback = result ->
	{
		DeferredObject<String> deferred = new DeferredObject<>();

		new Thread(() ->
		{
			isPiped = true;
			deferred.resolve("then result");
		}, "Then Thread").start();

		return deferred.promise();
	};

	@Before
	public void setUp() throws Exception
	{
		isDone = false;
		isFailed = false;
		isAlways = false;
		isMapped = false;
		isPiped = false;
		isFailedFailed = false;
		progress = 0;
		deferred = new DeferredObject<>();
	}


	//region - test done and resolve
	@Test
	public void test_done_returnsTheSamePromise() throws Exception
	{
		Promise promise = deferred.done(object -> {});

		Assert.assertEquals(promise, deferred);
	}

	@Test
	public void test_done_callbackIsNotExecutedIfNotResolved() throws Exception
	{
		deferred.done(object -> {
		});

		Assert.assertEquals(false, isDone);
	}

	@Test
	public void test_resolve_changesStateToResolved() throws Exception
	{
		deferred.resolve();

		Assert.assertEquals(Promise.State.Resolved, deferred.getState());
		Assert.assertTrue(deferred.isResolved());
	}

	@Test
	public void test_resolve_executesDoneCallback() throws Exception
	{
		deferred.done(doneCallback);

		Assert.assertFalse(isDone);
		deferred.resolve();
		Assert.assertTrue(isDone);
	}

	@Test
	public void test_resolve_resultIsNullWhenNotSpecified() throws Exception
	{
		deferred.resolve();

		Assert.assertNull(deferred.getResult());
	}

	@Test
	public void test_resolve_resultIsTheResolvedOne() throws Exception
	{
		deferred.resolve("hello");

		Assert.assertEquals("hello", deferred.getResult());
	}


	@Test
	public void test_done_executesCallbackImmediatelyWhenPromiseIsResolvedAlready() throws Exception
	{
		deferred.resolve();
		deferred.done(doneCallback);

		Assert.assertTrue(isDone);
	}

	//endregion


	//region - test fail and reject
	@Test
	public void test_fail_returnsTheSamePromise() throws Exception
	{
		Promise promise = deferred.fail(failCallback);

		Assert.assertEquals(promise, deferred);
	}

	@Test
	public void test_reject_changesStateToRejected() throws Exception
	{
		deferred.reject(new RuntimeException());

		Assert.assertEquals(Promise.State.Rejected, deferred.getState());
		Assert.assertTrue(deferred.isRejected());
	}

	@Test
	public void test_reject_callsFailCallback() throws Exception
	{
		deferred.fail(failCallback);

		deferred.reject(new RuntimeException());
		Assert.assertTrue(isFailed);
	}

	@Test
	public void test_reject_causeIsTheRejectedReason() throws Exception
	{
		RuntimeException reason = new RuntimeException();
		deferred.reject(reason);

		Assert.assertEquals(reason, deferred.getCause());
	}

	@Test
	public void test_fail_callbackIsExcecutedImmediatelyWhenPromiseIsRejectedAlready() throws Exception
	{
		deferred.reject(new RuntimeException());
		deferred.fail(failCallback);

		Assert.assertTrue(isFailed);
	}
	//endregion


	//region - progress
	@Test
	public void test_progress_returnsTheSamePromise() throws Exception
	{
		Promise promise = deferred.progress(progressCallback);

		Assert.assertEquals(promise, deferred);
	}

	@Test
	public void test_setProgress_callsProgressCallback() throws Exception
	{
		deferred.progress(progressCallback);

		deferred.setProgress(20);

		Assert.assertEquals(20, (int) progress);
	}

	@Test
	public void test_progress_callsCallbackWith100WhenResolved() throws Exception
	{
		deferred.resolve();
		deferred.progress(progressCallback);


		Assert.assertEquals(100, (int) progress);
	}
	//endregion


	//region - always
	@Test
	public void test_always_returnsTheSamePromise() throws Exception
	{
		Promise promise = deferred.always(alwaysCallback);

		Assert.assertEquals(promise, deferred);
	}

	@Test
	public void test_always_calledWhenResolved() throws Exception
	{
		deferred.always(alwaysCallback);
		deferred.resolve();

		Assert.assertTrue(isAlways);
	}

	@Test
	public void test_always_calledWhenRejected() throws Exception
	{
		deferred.always(alwaysCallback);
		deferred.reject(new RuntimeException());

		Assert.assertTrue(isAlways);
	}
	//endregion


	//region - map
	@Test
	public void test_map_returnsNewPromise() throws Exception
	{
		Promise promise = deferred.map(mapCallback);

		Assert.assertNotEquals(promise, deferred);
	}

	@Test
	public void test_resolve_executesMapCallback() throws Exception
	{
		 deferred.map(mapCallback);
		deferred.resolve();

		Assert.assertTrue(isMapped);
	}

	@Test
	public void test_resolve_makesMapPromiseResolved() throws Exception
	{
		Promise promise = deferred.map(mapCallback);
		deferred.resolve();

		Assert.assertTrue(promise.isResolved());
		Assert.assertEquals("map result", promise.getResult());
	}

	@Test
	public void test_resolve_makesMapPromiseRejectedWhenCallbackThrowsError() throws Exception
	{
		Exception error = new RuntimeException();
		Promise promise = deferred.map(result ->
		{
			throw error;
		});

		deferred.resolve();

		Assert.assertTrue(promise.isRejected());
		Assert.assertEquals(error, promise.getCause());
	}

	@Test
	public void test_reject_makesMapPromiseRejected() throws Exception
	{
		Exception error = new RuntimeException();
		Promise promise = deferred.map(mapCallback);
		deferred.reject(error);

		Assert.assertFalse(isMapped);
		Assert.assertTrue(promise.isRejected());
		Assert.assertEquals(error, promise.getCause());
	}

	//endregion

	//region - then
	@Test
	public void test_then_returnsNewPromise() throws Exception
	{
		Promise promise = deferred.then(pipeCallback);

		Assert.assertNotEquals(promise, deferred);

	}

	@Test
	public void test_resolve_executesThenPipeCallback() throws Exception
	{
		deferred.then(pipeCallback);
		deferred.resolve();

		Thread.sleep(10);
		Assert.assertTrue(isPiped);
	}

	@Test
	public void test_resolve_executesThenDoneCallback() throws Exception
	{
		deferred.then(doneCallback);
		deferred.resolve();

		Assert.assertTrue(isDone);
	}

	@Test
	public void test_then_chainExpression() throws Exception
	{
		List<String> result = new ArrayList<>();

		deferred.then(object ->
		{
			return new DeferredObject<String>().resolve("world");

		}).then(object ->
		{
			result.add(object);

		}).then(object ->
		{
			return new DeferredObject<Integer>().resolve(3);
		});

		deferred.resolve();

		Assert.assertEquals(1, result.size());
	}

	@Test
	public void test_resolve_makesThenPromiseResolved() throws Exception
	{
		Promise promise = deferred.then(pipeCallback);
		deferred.resolve();

		Assert.assertTrue(promise.isResolved());
		Assert.assertEquals("then result", promise.getResult());

	}

	@Test
	public void test_resolve_makesThenPromiseRejectedWhenCallbackThrowsError() throws Exception
	{
		RuntimeException error = new RuntimeException();
		DoneCallback callback = result ->
		{
			throw error;
		};
		Promise promise = deferred.then(callback);

		deferred.resolve();

		Assert.assertTrue(promise.isRejected());
		Assert.assertEquals(error, promise.getCause());
	}

	@Test
	public void test_reject_makesThenPromiseRejected() throws Exception
	{
		Exception error = new RuntimeException();
		Promise promise = deferred.then(pipeCallback);
		deferred.reject(error);

		Assert.assertFalse(isPiped);
		Assert.assertTrue(promise.isRejected());
		Assert.assertEquals(error, promise.getCause());

	}

	@Test
	public void test_then_OKwhenReturnsNull() throws Exception
	{
		deferred.then(text -> null).fail(failCallback);

		deferred.resolve();

		Assert.assertFalse(isFailed);
	}
	//endregion

	//region - all
	@Test
	public void test_all_resolvedWhenAllPromisesAreResolved() throws Exception
	{
		DeferredObject<Integer> intDeferred = new DeferredObject<>();
		DeferredObject<String> strDeferred = new DeferredObject<>();

		Promise promise = DeferredObject.all(intDeferred, strDeferred);

		Assert.assertFalse(promise.isResolved());

		intDeferred.resolve(2);
		Assert.assertFalse(promise.isResolved());

		strDeferred.resolve("hello");
		Assert.assertTrue(promise.isResolved());
	}

	@Test
	public void test_all_rejectedWhenOneOfThemIsRejected() throws Exception
	{
		DeferredObject<Integer> intDeferred = new DeferredObject<>();
		DeferredObject<String> strDeferred = new DeferredObject<>();

		Promise promise = DeferredObject.all(intDeferred, strDeferred);

		intDeferred.reject(new RuntimeException());
		Assert.assertTrue(promise.isRejected());
	}
	//endregion

	@Test
	public void test_chainWithException() throws Exception
	{
		final StringBuffer sb = new StringBuffer();
		deferred.then(text ->
		{
			isPiped = true;

			sb.append(text);
			if(true) {
				// throw exception
				throw new RuntimeException();
			}

			return new DeferredObject<String>().resolve("world");

		}).fail(cause ->
		{
			// called
			isFailed = true;

		}).map(text ->
		{
			// not called
			isMapped = true;
			sb.append(text);
			return "mapped";

		}).done(text ->
		{
			// not called
			sb.append(text);

		}).fail(cause ->
		{
			// called
			isFailedFailed = true;
		}).always(() ->
		{
			// called
			isAlways = true;
		});

		deferred.resolve("hello");

		Assert.assertTrue(isFailed);
		Assert.assertTrue(isFailedFailed);
		Assert.assertFalse(isMapped);
		Assert.assertTrue(isPiped);
		Assert.assertTrue(isAlways);
		Assert.assertEquals("hello", sb.toString());
	}


}
package com.enblink.jpromise;

import java.util.concurrent.atomic.AtomicInteger;

public class MultipleDeferredObject extends DeferredObject<Object[]>
{
	private final int numberOfPromises;
	private final AtomicInteger doneCount = new AtomicInteger();
	Object[] results;

	/**
	 * Executes all promises simultaneously.
	 *
	 * This promise is resolved when all promises are resolved.
	 * This promise is rejected when one of promises is rejected.
	 *
	 * @param promises the array of promises
	 */
	public MultipleDeferredObject(Promise... promises)
	{
		if (promises == null || promises.length == 0)
			throw new IllegalArgumentException("Promises is null or empty");

		this.numberOfPromises = promises.length;

		results = new Object[numberOfPromises];

		int count = 0;
		for (final Promise promise : promises)
		{
			final int index = count++;

			promise.done(result ->
			{
				synchronized (MultipleDeferredObject.this)
				{
					if(MultipleDeferredObject.this.isResolved()) return;

					results[index] = result;

					int done = doneCount.incrementAndGet();
					if (done == numberOfPromises)
					{
						MultipleDeferredObject.this.resolve(results);
					}
				}
			}).fail(cause ->
			{
				synchronized (MultipleDeferredObject.this)
				{
					if (MultipleDeferredObject.this.isRejected()) return;

					MultipleDeferredObject.this.reject(cause);
				}
			});

		}
	}
}

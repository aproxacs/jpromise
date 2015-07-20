package com.enblink.jpromise.callbacks;

import com.enblink.jpromise.Promise;

@FunctionalInterface
public interface PipeCallback<T, R>
{
	Promise<R> onDone(final T result) throws Throwable;
}

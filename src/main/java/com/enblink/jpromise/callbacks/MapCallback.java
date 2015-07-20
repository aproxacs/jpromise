package com.enblink.jpromise.callbacks;

@FunctionalInterface
public interface MapCallback<T, R>
{
	R onDone(final T result) throws Throwable;
}

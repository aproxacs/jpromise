package com.enblink.jpromise.callbacks;

@FunctionalInterface
public interface DoneCallback<T>
{
	void onDone(final T result);
}

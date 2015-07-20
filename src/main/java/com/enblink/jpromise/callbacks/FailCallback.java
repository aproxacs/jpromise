package com.enblink.jpromise.callbacks;

@FunctionalInterface
public interface FailCallback
{
	void onFail(final Throwable cause);
}

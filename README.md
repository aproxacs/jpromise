# Overview
JPromise is the implementation of deferred/promise pattern of node.js.

According to the [wiki](https://en.wikipedia.org/wiki/Futures_and_promises) Promise is writable and Future is read-only.
JAVA implements it as [Future](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html). 
And Netty implement it as [Future](http://netty.io/4.0/api/io/netty/util/concurrent/Future.html) and [Promise](http://netty.io/4.0/api/io/netty/util/concurrent/Promise.html)
But node.js sees Promise as read-only object. and DeferredObject as writable. It follows node.js concept.

JPromise is very similar to [JDeferred](https://github.com/jdeferred/jdeferred). The main difference is that it uses netty's
promise implementation. It gives JPromise the ability to choose the thread which the callback will be executed in. 

It only supports JAVA 8. I love lambda expression. 

# requirements
- JAVA 8
- netty

# Maven
NOT yet supported.  
you have to build the artifact by yourself.

1. download the sources
2. build an artifact using mvn

```
mvn clean;mvn install
```

# Examples
## Basic
```java
DeferredObject<Integer> deferred = new DeferredObject<>();
Promise promise = deferred;
promise.done(result ->
{
    System.out.print(result); // => 3
    // handle result
})
.fail(cause ->
{
    // handle error
});

// ...
deferred.resolve(3);
```

## then and map
then() and map() returns new promise. 
If an exception occurs, automatically promise turns rejected and fail callback is executed.

If then callback returns value, it is regarded as a PipeCallback. Return value have to be a Promise.
If not it is Donecallback.

You can use map method with map callback whan you want to return a value, not promise.

My hope is to use then method all the time(without using map method), but I could not find out how to do that with lambda expression.

 
```java
DeferredObject<Integer> deferred = new DeferredObject<>();
Promise<Integer> promise = deferred;
promise.then(count ->
{
	if(count < 0) throw new RuntimeException("count cannot be less than 0");

	String result = "hello " + count;
	return new DeferredObject<String>().resolve(result);

}).map(str ->
{
	System.out.println(str); // => hello 2
	return 3.9;

}).then(number ->
{
	System.out.println(number); // => 3.9
	
}).fail(error ->
{
	// handle error
});

// ...
deferred.resolve(2);

```

## progress
```java
DeferredObject<Integer> deferred = new DeferredObject<>();
Promise<Integer> promise = deferred;
promise.progress(progress -> 
{
    System.out.print(progress); // => 23
});

deferred.setProgress(23);

```

## Multiple Promises
```java
Promise promise = DeferredObject.all(promise1, promise2);
promise.done(results -> 
{
    Object result1 = results[0];
    Object result2 = results[1];
});
```

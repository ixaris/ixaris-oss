# Asynchronous Library

This library provides a number of utilities for asynchronous execution of code

## Async

Using Async, code can be written in a seemingly synchronous fashion. Such code is then transformed
to equivalent asynchronous code by the transformer. See [`ix-commons-async-transformer`](../transformer/README.md).

Asynchronous methods should return `Async<>` and use `Async.await()` to compose asynchronous results.
In addition, concurrent asynchronous results can be composed in a single result using overloaded
methods `Async.all()` and `Async.allSame()`. Below are some examples:

```java
public Async<A> asyncOperationA() throws AException {
    ...
}

public Async<B> asyncOperationB() throws BException {
   ...
}

public Async<C> asyncOperationC(final A a, final B b) throws CException {
   ...
}

public Async<Void> compoundOperation() {
    try {
        // using all() to compose concurrent asynchronous task results into 1 result
        final Tuple2<A, B> asyncTuple = await(all(asyncOperationA(), asyncOperationB()));
        
        // using awaitResult() to be able to handle CException otherwise it is not really thrown
        return awaitResult(asyncOperationC(asyncTuple.get1(), asyncTuple.get2())); 
    } catch (final AException | BException e) {
        throw new IllegalStateException(e);
    } catch (final CException e) {
        return result(DEFAULT);
    }
}
```

Since this async/await functionality is not a language feature there are some restrictions. Ideally, the 
IDE / compiler would stop incorrect use. The primary point of confusion is exception handling. An `Async<>` 
returning method may throw exceptions (even checked ones) but these exceptions are actually thrown when 
`Async.await()` is called. 

- `Async<>` types are not allowed as parameters, use `CompletionStage<>` instead and use `Async.async()` to 
convert between these 
- Methods that do not return `Async<>` are not allowed to call `Async.await()`. If you really need to wait for 
the result in such methods, then use `Async.block()`. For methods that return void, return `Async<Void>` instead
- If both methods `op()` and `async$op()` are declared in a class, they will not be transformed, as it is assumed
that either transformation was already done, or a handcoded asynchronous implementation was provided. In the 
latter case, the correct way to implement `op()` is to `throw Async.noTransformation()`
- Monitors obtained in synchronized blocks are released before `Async.await()` and reacquired after. Other locks 
should be managed explicitly. In particular, acquiring a lock before `Async.await()` and releasing after means that
the lock is held until the future is resolved. Locking is discouraged in favor of partitioning and queues, e.g.
using `AsyncQueue`.   
- Exceptions declared by `op()` are actually thrown by `Async.await()` so try..catch blocks will only work around 
`Async.await()`. This also applies when converting to `CompletionStage<>` by using `Async.async(op())`, where the 
exceptions declared by `op()` will never be thrown, but the compiler will still require you to handle such checked
exception (such declared exceptions could be the cause of rejection). 

As such, The so the following two cases are **WRONG**:

```java
try {
   Async<Void> a = op();
} catch (final SomeException e) {
   // it seems as though the exception will be caught and handled
   // but this exception will not really be thrown by op() unless await() is used
   ...
}
await(a); // exception is really thrown here

try {
   return op(); // should return Async.awaitResult(op());
                // the await() causes the exception to be handled in this try..catch block
} catch (final SomeException e) {
   // it seems as though the exception will be caught and translated but it will not
   // but this exception will not really be thrown by op() unless await() is used
   throw new SomeOtherException(e);
}
```
   
The **CORRECT** way to implement the above is as follows:

```java
try {
   a = await(op());
} catch (final SomeException e) {
   ...
}

try {
   return awaitResult(op());
} catch (final SomeException e) {
   throw new SomeOtherException(e);
}
```

### Testing with Async

Since test methods need to return void, one cannot `await()` during a test. It is recommended to use `Async.block()`
to block waiting for results. For assertions, it is recommended to convert `Async<>` to `CompletionStage<>` using
`Async.async()` and use `CompletionStageAssert` in [`ix-commons-async-test`](../test/README.md).

## Async Locals

Async locals work similar to thread locals but across an async process. The following is an example of setting
and retrieving the value of an `AsyncLocal`:

```java
...
public static final AsyncLocal<String> SOME_VALUE = new AsyncLocal<>();
...
// set a value for the async local
SOME_VALUE.exec("value", () -> {
...
    // read the value of the async local from within the enclosing block or 
    // from a task spawned within the enclosing block
    doSomethingWithValue(SOME_VALUE.get());
...
});
```

For async locals to actually be propagated to asynchronous tasks, the tasks need to be wrapped using 
`AsyncLocal.wrap()` as follows:

```java
executor.execute(AsyncLocal.wrap(() -> { ... }));

// Or simplified as:
AsyncExecutor.exec(executor, () -> { ... });
```

Alternatively, invoking code should take a snapshot and apply the snapshot in the asynchronous
task as follows:
 
```java
final Snapshot snapshot = AsyncLocal.snapshot();
...
// use this in place of the task
executor.execute(() -> AsyncLocal.exec(snapshot, () -> {
    ...
}));
```

### Stacking

Async Local value can be stackable. If stackable, values are pushed to a stack and popped 
afterwards, otherwise, an error is thrown if a value is already set.

## Async Trace

Asynchronous processes are notoriously hard to debug because the stack traces do not show the 
whole process, but only the last synchronous leg. To mitigate this, the AsyncTrace class
attaches the stack of the process forking an asynchronous task to the task itself such that on
exception, the task can attach the trace as it's cause. Traces for exceptions throws from
`Async<>` returning methods are automatically joined. Use as follows:

```java
final CompletableFuture<?> future = new CompletableFuture<>();
executor.execute(AsyncTrace.wrap(() -> {
    ...
    try {
        ...
    } catch (final Throwable t) {
        // AsyncTrace.join() joins the caller's stack to the exception stack
        future.completeExceptionally(AsyncTrace.join(t));
    }
}));

// Or simplified as:
AsyncExecutor.exec(executor, () -> { ... });
// which wraps the task and automatically joins the traces for thrown exceptions.
```

This produces stack traces similar to the following (stack trace from `AsyncTraceTest`):

```
java.lang.IllegalStateException
	at com.ixaris.commons.async.lib.AsyncTraceTest.async$execute(AsyncTraceTest.java:45)
	at com.ixaris.commons.async.lib.AsyncTraceTest.async$lambda$execute$7(AsyncTraceTest.java:47)
	at com.ixaris.commons.async.lib.AsyncExecutor.lambda$exec$5(AsyncExecutor.java:123)
	at com.ixaris.commons.async.lib.AsyncLocal.lambda$exec$16(AsyncLocal.java:188)
	at com.ixaris.commons.async.lib.AsyncLocal.executeAndRestoreAsyncLocals(AsyncLocal.java:197)
	at com.ixaris.commons.async.lib.AsyncLocal.exec(AsyncLocal.java:183)
	at com.ixaris.commons.async.lib.AsyncLocal.exec(AsyncLocal.java:187)
	at com.ixaris.commons.async.lib.AsyncLocal.lambda$wrap$1(AsyncLocal.java:127)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.lambda$exec$0(ThreadLocalHelper.java:72)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.exec(ThreadLocalHelper.java:64)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.exec(ThreadLocalHelper.java:71)
	at com.ixaris.commons.async.lib.AsyncTrace.exec(AsyncTrace.java:83)
	at com.ixaris.commons.async.lib.AsyncTrace.lambda$wrap$1(AsyncTrace.java:30)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.lambda$exec$0(ThreadLocalHelper.java:72)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.exec(ThreadLocalHelper.java:64)
	at com.ixaris.commons.async.lib.thread.ThreadLocalHelper.exec(ThreadLocalHelper.java:71)
	at com.ixaris.commons.async.lib.AsyncExecutor.lambda$wrap$0(AsyncExecutor.java:46)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
Caused by: com.ixaris.commons.async.lib.AsyncTrace: Async Trace [2] @ pool-1-thread-1
	at com.ixaris.commons.async.lib.AsyncTrace.get(AsyncTrace.java:76)
	at com.ixaris.commons.async.lib.AsyncTrace.wrap(AsyncTrace.java:29)
	at com.ixaris.commons.async.lib.AsyncExecutor.async$exec(AsyncExecutor.java:121)
	at com.ixaris.commons.async.lib.AsyncTraceTest.async$execute(AsyncTraceTest.java:47)
	... 19 more
Caused by: com.ixaris.commons.async.lib.AsyncTrace: Async Trace [1] @ pool-1-thread-1
	at com.ixaris.commons.async.lib.AsyncTrace.get(AsyncTrace.java:76)
	at com.ixaris.commons.async.lib.AsyncTrace.wrap(AsyncTrace.java:29)
	at com.ixaris.commons.async.lib.AsyncExecutor.async$exec(AsyncExecutor.java:121)
	at com.ixaris.commons.async.lib.AsyncTraceTest.async$execute(AsyncTraceTest.java:47)
	at com.ixaris.commons.async.lib.AsyncTraceTest.async$lambda$testLogging$0(AsyncTraceTest.java:26)
	... 18 more
Caused by: com.ixaris.commons.async.lib.AsyncTrace: Async Trace [0] @ main
	at com.ixaris.commons.async.lib.AsyncTrace.get(AsyncTrace.java:76)
	at com.ixaris.commons.async.lib.AsyncTrace.wrap(AsyncTrace.java:29)
	at com.ixaris.commons.async.lib.AsyncExecutor.async$exec(AsyncExecutor.java:121)
	at com.ixaris.commons.async.lib.AsyncTraceTest.testLogging(AsyncTraceTest.java:26)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)
```

Should tracing cause problems, in particular, possible memory problems for very deep processes, this can 
be disabled by setting system property `async.trace.skip` to `true`
 
## Async Executor

Some tasks need to be executed on different executors, depending on the nature of the work. Typically
computation is executed separately from I/O. For this reason, I/O tasks typically will need to relay 
the computation back to the computation executor.
 
By wrapping an executor in an AsyncExecutorWrapper, each thread will track its executor, which lets 
code relay result processing tasks back to the originating executor, as follows:

```java
final Executor originalExecutor = AsyncExecutor.get();
ioExecutor.execute(() -> {
    // assuming some code gets a result using blocking I/O, the next line relays computation 
    // back to the original executor
    originalExecutor.execute(() -> {
        // do something with result
    });
});

// Or simplified as:
result = Async.await(AsyncExecutor.relay(() -> AsyncExecutor.exec(ioExecutor, () -> { ... })));
// which relays the future from the given executor back to the executor associated with the
// original thread
```

## Async Queue

The async queue can be used to control concurrent access to resources by queueing such access.


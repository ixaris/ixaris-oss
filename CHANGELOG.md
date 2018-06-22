# 1.6.0

# 1.5.0 (2018-06-22)

## New Features
* Async: Unified exec() and schedule() overloaded methods (and renamed execSync to exec and scheduleSync to schedule)
* Async: added relayWithContext() to AsyncExecutor to enable interoperability with asynchronous APIs that lose context (trace, locals)
* Async: tweaks to async connection pool, specifically, removed the map of connections in use and using a simpler way of tracking usage
* Misc: Added unsafe helper

## Bug Fixes
* Async: Fixed AsyncLocal exec() methods to correctly restore previous async locals in futures returned from within a block with async locals applied (with added test)

# 1.4.1 (2018-05-14)

## Bug Fixes
* Async: Reintroduced deprecated relay() methods in AsyncExecutor to maintain backward compatibility

# 1.4.0 (2018-05-11)

## New Features
* Async: Various simplifications to the API
* Async: Removed requirement to use async to convert `Async` to `CompletionStage`
* Async: Can now annotate a method that returns custom `CompletionStage` subclass with `@Async` and use `await()`
* Async: Simplified transformer by transforming less code

# 1.3.0 (2018-05-04)

## New Features
* Async: simplified exception handling transformation by letting exceptions be throws instead of rejecting async
* Async: java 10 support using AsyncLambdaMetafactory and ugly reflection + asm hack

# 1.2.0 (2018-04-05)

## Bug Fixes
* Async: Fixed issue with Thread Locals being lost during execution 

# 1.1.0 (2018-04-04)

## New Features
* Deploy to maven central
* Async: Transformer now supports eclipse/groovy compiler not just javac

## Bug Fixes
* Async: Fixed wrong instrumentaion of infinite loop at the start of a method (with test)

# 1.0.0 (2018-04-06)

## New Features
* First Release
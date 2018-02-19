# Asynchronous Testing Library

This library provides an assertion class to aid in asserting on completion stages. To use in conjunction with `Async<>`
returning methods, transform the result to a `CompletionStage<>` using `Async.async()`. 
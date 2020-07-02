# Persistence Library

This library provides abstractions around persistence and ACID transactions, as well as idempotency
  
## idempotency

Idempotency is based on the concept of an *intent*. An intent represents a unique intent to perform a state 
changing operations. Read operations do not need an intent as they do not change the system state.
 
An intent is the combination of:
- a unique id
- a string representing the action and the resource (typically a method and path)
- a hash of the input params

A process shares the intent id. However, this is different from the correlation id. During retries, the intent id 
is the same whereas the correlation id is different.

### Implementing Idempotency

Microservice calls should be idempotent. This means that if we are unsure about the outcome of an operation and 
we retry it, then the operation side-effects should be effected only once. The result may or may not be the same,
as idempotency only applies to the change in system state, i.e. the side effect. If the call was successful the 
first time, retrying it should return the already created data, WITHOUT creating anything new. If on the other 
hand the call resulted in a conflict, then retrying may either result in the same conflict or in a result, 
provided that the original conflict did not cause a side effect. If it did, then the same conflict MUST be 
returned.

Handling of idempotency is as follows:
- read only operations return freshest result (be it 200 / 404 / 409 etc)
- state changing operations
  - persist intent (+ initial state if any) immediately
  - if ok
    - proceed with process, updating state for every checkpoint.
    - in case of error, for operations that are log append based, persist conflict with immutable log in finite state, otherwise, 
      delete intent if the state is not modified or write an immutable records of the failure 
  - else (if intent already exists)
    - lookup entity and check state
    - proceed after last persisted checkpoint
    - if process complete, return result (typically fresh lookup) or failure accordingly

**Example**: if we request the creation of a profile, the profile gets created, but we do not receive a response, 
when we retry the call, it should not re-create a second profile, rather it should return the one created in the
previous call. 

The important thing to consider here is the *intent* of the call, and to ensure that every intent is only 
performed once. 

- An **intent identifier** is passed along each request. It is up to the caller to ensure that parameters in 
 subsequent requests match previous requests exactly. When calling other microservices from another microservice, 
 make sure to use same parameters when repeating calls with the same intent.

#### Additional notes
 
- If the request cannot be served in one atomic operation (for example, you need to call another microservice), 
you should have your own book-keeping to record whether the intent was fully processed or not. 
- Only mark the intent as being complete (persist intent or update completed flag) **iff** the request has 
resulted in either a side effect
- Make sure that setting the intent to complete happens as part of the transaction that applies the side effect
in order to avoid situations where the side effect is done but the intent is not updated, or vice versa
- If you're performing a multi step process, mark the intent as complete when all steps are done. Alternatively, 
consider having compensating steps.
- When an endpoint is unsecured, any authorisation checks have to be bypassed. For example, if you are handling 
a retried creation call (same intent id, thus should return the result), make sure that the lookup used during
retry does not perform any authorisation.
- **Implementation of idempotency varies between microservices**. These are some general guidelines, but don't 
be limited by what is defined here (within reason)
 

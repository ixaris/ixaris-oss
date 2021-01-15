# Dimensional Counters Library

This library provides a way to manage context based counters. To use this library, 
microservice must provide the required schema by including the provided migration steps 
to their own migration scripts.

## Defining a counter

A counter's context defines the dimensions. The context first defines the partitioning dimensions.
These are the dimensions whose value determines which node should check and update the counter.
Routing is done by implementing the interface `com.ixaris.commons.dimensions.counters.cluster.CounterPartitioning`,
which should route the request to the node handling a counter instance based on the context.

A counter is defined by extending `AbstractCounterDef`. A counter defines:
- the context (starting from the partitioning dimensions, followed by cartesian product dimensions followed by the rest)
- the table that holds the counter values and context
- helper methods to aggregate counter windows for seeding new counters


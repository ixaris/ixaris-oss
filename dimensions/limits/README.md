# Dimensional Limits Library

This library provides a way to manage context based limits. To use this library, 
microservice must provide the required schema by including the provided migration steps 
to their own migration scripts.

There are 2 types of limits: value and counter

## Value Limits

Value limits are limits against a known value, e.g. max transaction amount or expiry period. These work in a very
similar way as configuration properties. However, the way they are looked up differs. For a given context, the bounds
are the lower amount and the upper amount and count limits of all matching limits.

As an example, if a limit is defines as [A, *] = (0-100) and [*, B] = (10-200) then the bounds for [A, B] are (10-100)

## Counter Limits

Counter limits are limits against a counter. The bounds are determined as for value limits, but they also define the
windows parameters for the counter they are checked against.

## Extension

Limits support extending the information persisted by supplying an extension entity 

## Administration

The library provides protobuf objects to build a microservice admin api, and a helper
around the administration operations. The SCSL would look similar to the following:

```yaml
/limits:
  /value:
    metadata:
      responses:
        success: com.ixaris.commons.dimensions.config.ValueLimitDefs
    /{value_limit_key}:
      parameter: string
      metadata:
        responses:
          success: com.ixaris.commons.dimensions.config.ValueLimitDef
      get:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.Limit
      all_matching:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.ValueLimits
      all_containing:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.ValueLimits
      set:
        request: com.ixaris.commons.dimensions.lib.ValueLimit
      /{value_limit_id}:
        parameter: int64
        get:
          responses:
            success: com.ixaris.commons.dimensions.limits.ValueLimit
  /counter:
    metadata:
      responses:
        success: com.ixaris.commons.dimensions.config.CounterLimitDefs
    /{counter_limit_key}:
      parameter: string
      metadata:
        responses:
          success: com.ixaris.commons.dimensions.config.CounterLimitDef
      get:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.Limit
      all_matching:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.CounterLimits
      all_containing:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.limits.CounterLimits
      set:
        request: com.ixaris.commons.dimensions.lib.CounterLimit
      /{counter_limit_id}:
        parameter: int64
        get:
          responses:
            success: com.ixaris.commons.dimensions.limits.CounterLimit

```

# Dimensional Configuration Library

This library provides a way to manage context based configuration. To use this library, 
microservice must provide the required schema by including the provided migration steps 
to their own migration scripts.

## Glossary of terms

| Term            | Definition|
| ----            | ----------|
| Value           | A configurable value, potentially having different values for different contexts|
| Set             | A configurable set of values|
| Incremental Set | A configurable set built from all matching sets, typically used for e.g. whitelists|
| Dimension       | A named part of the context|
| Context         | A collection of dimensions, ordered by specificity|
| Matching        | Retrieving values based on whether the configured context matches the given context, e.g. configured (A, *) matches (A, B)|
| Containing      | Retrieving values based on whether the configured context contains the given context, e.g. configured (A, B) contains (A, *)|

## Context Depth Calculation

- 64-bit mask based on which dimensions are set
- Independent of the actual value of the dimension (but dependent on the depth in the hierarchy for hierarchical values)
- First bit is always 0 (as is signed and always positive, for comparison)
- More specific > less specific

##### Example normal (depth in HEX, in db will be BIGINT in decimal)

Property with dimensions (A, B).

| Context | - | - | - | - | - | - | - | - |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| [*, *] | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 |
| [A, *] | 01000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 |
| [*, B] | 00100000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 |
| [A, B] | 01100000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 |


##### Example with hierarchy (depth in HEX, in db will be BIGINT in decimal)

Property with dimensions (A, B), where A is hierarchical with depth 2; APARENT with child ACHILD

| Context | - | - | - | - | - | - | - | - |
| ---------  | ---  | --- | --- | --- | --- | --- | --- | --- |
| [*, *]       | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000
| [APARENT, *] | 00100000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000
| [ACHILD, *]  | 01000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000
| [*, B]       | 00010000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000
| [APARENT, B] | 00110000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000
| [ACHILD, B]  | 01010000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000 | 00000000


## Backward compatible changes

Changes to the context definition for sets and values should be done in a backward compatible way, such that
a release can be done with 0 downtime. As such, the data should remain valid for the service during a phased
release where the previous and current version of the service will run concurrently for a period of time.

The following can be done at any time:
- adding dimensions at the end (less specific)
- increasing the hierarchy depth from 2 up to 3, from 4 up to 7, from 8 up to 15 such that the required bits in the
  bitmap does not change

The following cannot be done, and require a new value / set to be defined, and migrated to:
- adding dimensions in between existing existing dimensions (will change the bitmap of previous contexts)
- increasing the hierarchy depth from  3 to 4 and 7 to 8 etc, such that the required bits in the context hash
  bitmap for that dimension will need to be increased
- changing the key of a dimension, as the old node will not know of the key during a phased migration

## Administration

The library provides protobuf objects to build a microservice admin api, and a helper
around the administration operations. The SCSL would look similar to the following:

```yaml
/config:
  /values:
    metadata:
      responses:
        success: com.ixaris.commons.dimensions.config.ConfigValueDefs
    /{value_key}:
      parameter: string
      simulate:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.Value
      get:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.Value
      all_matching:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigValues
      all_containing:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigValues
      set:
        request: com.ixaris.commons.dimensions.config.ConfigValue
      remove:
        request: com.ixaris.commons.dimensions.lib.Context
      metadata:
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigValueDef
  /sets:
    metadata:
      responses:
        success: com.ixaris.commons.dimensions.config.ConfigSetDefs
    /{set_key}:
      parameter: string
      simulate:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.Set
      get:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.Set
      all_matching:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigSets
      all_containing:
        request: com.ixaris.commons.dimensions.lib.Context
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigSets
      set:
        request: com.ixaris.commons.dimensions.config.ConfigSet
      remove:
        request: com.ixaris.commons.dimensions.lib.Context
      metadata:
        responses:
          success: com.ixaris.commons.dimensions.config.ConfigSetDef
```

- The implementation of `config/values/get` and `config/sets/get` should call the `toProtobuf()`
  method of `ValueDefRegistry` or `SetDefRegistry` respectively.
- The implementation of operations for `config/values/{value_key}` and `config/sets/{set_key}`
  should use an instance of `ConfigAdminProto`, typically created as a spring bean.
- The `get` operation uses the method `getExactMatchValue()` or `getExactMatchSet()`.
- The `set` operation uses the method `setConfigValue()` or `setConfigSet()`.
- The `remove` operation uses the method `removeConfigValue()` or `removeConfigSet()`.

Should the set methods throw a `ConfigValidationException`, return an client invalid request (400) with
the contained message. `IllegalArgumentException` should also be treated as an invalid
request (typically caused by an invalid dimension or value).


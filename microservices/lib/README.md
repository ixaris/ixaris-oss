# Microservice Library

## Conversions between Protobuf and JSON
The `MessageHelper` class provides convenience methods to convert between
Protobuf and JSON. Due to Protobuf's handling of default values, whereby
Protobuf does not distinguish between unset fields and fields set to their
default value, the conversion to JSON will automatically add all the
unset/default fields as their default value - this is in line with how
Protobuf would interpret such messages. The single exception to this is
nested messages. If the nested message is not set, this is not serialized.

For example, given the following message definitions:
```protobuf
message Example {
  int32 id = 1;
  bool boolean = 2;
  Nested nested = 3;
}

message Nested {
  string str = 1;
}
```
If the following message is serialized:
```java
Example.newBuilder().setId(123).build();
```
The JSON output would be as follows:
```json
{
  "id": 123,
  "boolean": false
}
```
Note that the nested message is not serialized since it was not set.
If it were set as below:
```java
Example.newBuilder().setId(123).setNested(Nested.newBuilder().build()).build();
```
The JSON output would be:
```json
{
  "id": 123,
  "boolean": false,
  "nested": {
    "str": ""
  }
}
```

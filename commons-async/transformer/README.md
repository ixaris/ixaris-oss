# Asynchronous Transformer

This transformer transforms code that uses `Async.await()` into stackless non-blocking continuations

To transform code, add the following dependency in maven:

```xml
<dependency>
    <groupId>com.ixaris.oss</groupId>
    <artifactId>ixaris-commons-async-transformer</artifactId>
    <version>VERSION</version>
    <classifier>nodeps</classifier>
    <optional>true</optional>
</dependency>
```

and make sure that annotation processing is not turned off in the compiler plugin configuration,
since the transformation is hooked into the compilation as an annotation processor.

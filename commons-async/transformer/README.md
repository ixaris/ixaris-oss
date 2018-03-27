# Asynchronous Transformer

This transformer transforms code that uses `Async.await()` into stackless non-blocking continuations

To transform code, add the following to the in maven pom.xml file:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.ixaris.oss</groupId>
                        <artifactId>ixaris-commons-async-transformer</artifactId>
                        <version>VERSION</version>
                        <classifier>nodeps</classifier>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Alternatively, make sure that annotation processing is not turned off in the compiler plugin configuration,
since the transformation is hooked into the compilation as an annotation processor, and add the below dependency:

```xml
<dependency>
    <groupId>com.ixaris.oss</groupId>
    <artifactId>ixaris-commons-async-transformer</artifactId>
    <version>VERSION</version>
    <classifier>nodeps</classifier>
    <optional>true</optional>
</dependency>
```


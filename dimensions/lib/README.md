# Dimensions Library

This library provides a way to create entities whose value is based on a context, made up of dimensions.

## Usage

Define you schema as follows:

```sql
CREATE TABLE property (
    id BIGINT NOT NULL,
    property_key VARCHAR(50) NOT NULL,
    context_depth BIGINT NOT NULL,
    ...
    PRIMARY KEY (id),
    KEY ix_example__example_key_context_depth (example_key, context_depth),
    ...
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;

CREATE TABLE property_dimension (
    id BIGINT NOT NULL,
    dimension_name VARCHAR(60) NOT NULL,
    long_value BIGINT DEFAULT NULL,
    string_value VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id, dimension_name),
    CONSTRAINT fk_property_dimension__id FOREIGN KEY (id) REFERENCES property(id),
    KEY ix_property_dimension__dimension_name_long_value (dimension_name, long_value),
    KEY ix_property_dimension__dimension_name_string_value (dimension_name, string_value(32))
) DEFAULT CHARSET=utf8 ROW_FORMAT=COMPRESSED;
```
Schema may be any data related to the context, e.g. properties whose value varies by context.

The main table must have:
- id, primary key
- key column (optional) to distinguish between different instances, may not be needed
- context_depth, a number that determines the specifity, or depth in the tree of contexts the bigger the number, the more specific

The dimensions table schema must match the above exactly:
- id, primary key and foreign key to main table
- dimension_name, long_value and string_value to store dimension details, with an index on name and both values

define dimensions like so (follow javadocs for more depth documentation):

```java
public class SomeDimensionDef extends StringDimensionDef<String> {
    
    private static final SomeDimensionDef INSTANCE = new SomeDimensionDef();
    
    public static SomeDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private SomeDimensionDef() {}
    
    @Override
    public String getStringValue(final String value) {
        return value;
    }
    
    @Override
    public String getValue(final String str, final Map<DimensionDef<?, ?>, Dimension<?>> contextMap) {
        return str;
    }
    
    @Override
    public boolean isCacheable() {
        return true;
    }
    
}
```


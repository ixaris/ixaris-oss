# Jooq Library Tile

This is a tile for code generation for libraries that provide persistence. Library tables should be 
prefixed with `lib_` and migration scripts placed in `resources/lib/migration`. These migrations are 
used as a template for services to include in their own migration.

To generate JOOQ library classes use the following command:

```
mvn clean install -Pjooq-generate
```

## Updating library schemas

**WARNING: Updating the schema will trigger the need of a schema migration of all services.  
Make sure to coordinate this accordingly.** 

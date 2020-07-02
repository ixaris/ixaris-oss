# Jooq Persistence Bindings

This library provides the jooq implementation of the persistence interfaces, as well as migration support 
using flyway.

## Entities

Entities wrap a record to simplify usage and hide mapping of values, e.g. enum to character and vice versa.
In addition, entities can manage relations between tables in a manner similar to hibernate.

An entity provides:
- A private constructor to set up the relations
- A package private constructor to construct an entity from existing records, used by the Repository for lookups
- One of more public constructors to create a new instance to persist. It is recommended that these constructors create 
detached records and attach in the `store()` and `delete()` methods. A detached record is created using the Record 
object's constructor (e.g. `new AuthorRecord()`) whereas an attached records is created using `context.newRecord(AUTHOR)` 
- Methods to get and update information
- implementation of the `store()` method to store the entity. Use `attachAndStore()` helper method to attach and store 
raw records, and `store()` method to store maps and optional relations
- If delete is supported, implementation if `delete()`
- If the entity will be placed in a cache or map, `equals()` and `hashCode()` implementation, considering only the
primary key 

 
### Relations

See `AuthorEntity` for examples of relations. For entities that support deletion, order of deletion is
important; relations should be deleted before the parent table to avoid referential integrity errors.

#### One to Many Relations

use `RecordMap` or `EntityMap` to manage a one to many relation. A map key is required, which may be
a field or computed from multiple fields. This keeps track of added, modified or removed records and flushed
the changes accordingly when `store()` is called. For map or set type relations where there is an obvious
unique value that identifies each item in the relation, this is a natural fit. If this is not the case, the
related item's primary key may be used as the map key. If a specific ordering of the related items is desired,
a map implementation may be supplied, e.g. a TreeMap with a custom comparator to order the items or a
LinkedHashMap for time series relations (in this latter case, an order by is required when fetching, to insert
the existing records in the correct order).  

```java
public final class AuthorEntity extends Entity<AuthorEntity> {
    
    private final AuthorRecord author;
    private final RecordMap<String, BookRecord> books;
    
    private AuthorEntity(final AuthorRecord author) {
        this.author = author;
        this.books = RecordMap.withNewRecordFunction(key ->
            new BookRecord(UniqueIdGenerator.generate(), author.getId(), key)
        );
    }
    
    // used by repository to create article from fetched records
    AuthorEntity(final AuthorRecord author, final Collection<BookRecord> books) {
        this(author);
        this.books.fromExisting(books, BookRecord::getName);
    }
    
    public AuthorEntity(final long id, <<creation parameters>>) {
        this(new AuthorRecord());
        this.author.setId(id);
        <<populate new author>>
    }
    
    @Override
    public AuthorEntity store() {
        attachAndStore(author);
        books.store();
        return this;
    }
    
    @Override
    public AuthorEntity delete() {
        books.delete(); // delete individual records (recommended for small relations in the tens of records) 
        // if (!books.getMap().isEmpty()) {
        //     JOOQ_TX.get().delete(BOOK).where(BOOK.AUTHOR_ID.equal(author.getId())).execute();
        // }
        attachAndDelete(author);
        return this;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Objects.equals(author.getId(), other.author.getId()));
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(author.getId());
    }
    
    ...
    
}

public class AuthorsRepository {
    
    public static AuthorEntity lookup(final long id) {
        return fetch(id).orElseThrow(EntityNotFoundException::new);
    }
    
    public static Optional<AuthorEntity> fetch(final long id) {
        return JOOQ_TX.get().fetchOptional(AUTHOR, AUTHOR.ID.eq(id)).map(AuthorsRepository::finishLoad);
    }
    
    public static List<AuthorEntity> findAll(final int offset, final int limit) {
        final SelectWhereStep<AuthorRecord> select = JOOQ_TX.get().selectFrom(AUTHOR);
        
        if (offset >= 0) {
            select.offset(offset);
        }
        if (limit > 0) {
            select.limit(limit);
        }
        
        return finishLoad(select.fetchMap(AUTHOR.ID));
    }
    
    ...
    
    private static AuthorEntity finishLoad(final AuthorRecord record) {
        return new AuthorEntity(record, JOOQ_TX.get().fetch(BOOK, BOOK.AUTHOR_ID.eq(record.getId())));
    }
    
    private static List<AuthorEntity> finishLoad(final Map<Long, AuthorRecord> map) {
        final Set<Long> ids = authorsMap.keySet();
        
        final Map<Long, Result<BookRecord>> books = JOOQ_TX.get()
            .selectFrom(BOOK)
            .where(BOOK.AUTHOR_ID.in(ids))
            .fetchGroups(BOOK.AUTHOR_ID);
        
        return map.values().stream()
            .map(author -> new AuthorEntity(author, books.get(author.getId())))
            .collect(Collectors.toList());
    }
    
    private AuthorsRepository() {}
    
}
```

Typical operations on maps are to synchronise with some given input or to add or remove some items.

To synchronise, use one of the overloads of `syncWith()`, which takes a collection and functions to extract
the key and apply the changes to the records. This method will remove any item not included in the given
collection, i.e. any key in the map that is not matched with the keys extracted from the given collection is
removed.
 
To add or update records, use one of the overloads of `update()`, which takes parameters similar to `syncWith()`
with the following differences: unmatched records are not removed, i.e. only the records that match the keys
extracted from the given collection are modified, the rest remain intact. In addition, the function to apply
changes to the records returns a boolean; returning true leaves the record in the map whereas returning false
removes the record from the map.

To remove records, use one of the overloads of `remove()`   

#### Optional One to One Relations

use `OptionalRecord` or `OptionalEntity` to manage an optional one to one relation. When fetching records
these relations are typically populated by left joining.

```java
public final class AuthorEntity extends Entity<AuthorEntity> {
    
    private final AuthorRecord author;
    private final OptionalRecord<AuthorExtraRecord> extra;
    
    private AuthorEntity(final AuthorRecord author) {
        this.author = author;
        this.extra = OptionalRecord.withNewRecordSupplier(() -> {
            final AuthorExtraRecord record = new AuthorExtraRecord();
            record.setId(author.getId());
            return record;
        });
    }
    
    AuthorEntity(final AuthorRecord author, final AuthorExtraRecord extra) {
        this(author);
        this.extra.fromExisting(extra);
    }
    
    public AuthorEntity(final long id, <<creation parameters>>) {
        this(new AuthorRecord());
        this.author.setId(id);
        <<populate new author>>
    }
    
    
    @Override
    public AuthorEntity store() {
        attachAndStore(author);
        extra.store();
        return this;
    }
    
    @Override
    public AuthorEntity delete() {
        extra.delete();
        attachAndDelete(author);
        return this;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Objects.equals(author.getId(), other.author.getId()));
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(author.getId());
    }
    
    ...
    
}

public class AuthorsRepository {
    
    public static AuthorEntity lookup(final long id) {
        return fetch(id).orElseThrow(EntityNotFoundException::new);
    }
    
    public static Optional<AuthorEntity> fetch(final long id) {
        return JOOQ_TX.get()
            .select()
            .from(AUTHOR)
            .leftJoin(AUTHOR_EXTRA)
            .on(AUTHOR_EXTRA.ID.eq(AUTHOR.ID))
            .where(AUTHOR.ID.eq(id))
            .fetchOptional()
            .map(AuthorsRepository::finishLoad);
    }
    
    public static List<AuthorEntity> findAll(final int offset, final int limit) {
        final SelectWhereStep<Record> select = JOOQ_TX.get()
            .select()
            .from(AUTHOR)
            .leftJoin(AUTHOR_EXTRA)
            .on(AUTHOR_EXTRA.ID.eq(AUTHOR.ID));
        
        if (offset >= 0) {
            select.offset(offset);
        }
        if (limit > 0) {
            select.limit(limit);
        }
        
        return finishLoad(select.fetchMap(AUTHOR.ID));
    }
    
    ...
    
    private static AuthorEntity finishLoad(final Record record) {
        final AuthorRecord author = record.into(AuthorRecord.class);
        return new AuthorEntity(
            author, 
            record.get(AUTHOR_EXTRA.ID) != null ? record.into(AuthorExtraRecord.class) : null
        );
    }
    
    private static List<AuthorEntity> finishLoad(final Map<Long, Record> map) {
        return map.values().stream()
            .map(record -> {
                final AuthorRecord author = record.into(AuthorRecord.class);
                return new AuthorEntity(
                    author,
                    record.get(AUTHOR_EXTRA.ID) != null ? record.into(AuthorExtraRecord.class) : null
                );
            })
            .collect(Collectors.toList());
    }
    
    private AuthorsRepository() {}
    
}
``` 

### Code Organisation

- /jooq
  - Jooq generated classes (records)
- /data
  - *Entity 
    - represents an entity group (e.g. encapsulate one to many relationships)
  - Repository
    - used to operations across instances, typically lookups but can also be bulk updates / deletes
- /
  - Service implementation
    - manages transaction and (potentially) interaction between several instances
    - locking / partitioning entities should be handled here
    - authorisation & idempotency should be handled here
  
## Idempotency

This library provides an implementation of the intent repository using a database table. To use this feature, 
microservice must provide the required schema by including the provided migration steps to their own migration 
scripts.

## Migration

Migration scripts must be placed in a folder db/migration. 
See [the flyway docs](https://flywaydb.org/documentation/migration/versioned) for information on how to name 
migration scripts.

### Libraries

If a library requires persistence to the database as part of its functionality, it needs to
generate the JOOQ classes itself. Any services that use the library should then make use of
the classes generated in the library. However, services will still have to define the schema
of the library in their own migration files.

### Important Notes
- Note that any tables starting with `lib_` are ignored during code generation, to avoid repeatedly
generating for all services that use a library
- If you make any changes to the schema, make sure that the old data can be migrated to the new schema. 
Consider phasing updates to allow for backward compatibility between phases.

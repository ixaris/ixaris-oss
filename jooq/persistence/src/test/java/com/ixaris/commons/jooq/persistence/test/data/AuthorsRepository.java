package com.ixaris.commons.jooq.persistence.test.data;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Article.ARTICLE;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.ArticleRelated.ARTICLE_RELATED;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Author.AUTHOR;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.AuthorExtra.AUTHOR_EXTRA;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Book.BOOK;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectWhereStep;

import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Author;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRelatedRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.AuthorExtraRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.AuthorRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.BookRecord;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

/**
 * Implements persistence layer for the entity Author. No idempotency implementation as it is a template for persistence proof of concept.
 */
public class AuthorsRepository {
    
    public static AuthorEntity fromProtobuf(final Author author) {
        return new AuthorEntity(author);
    }
    
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
    
    /**
     * An example to show how we would be applying find by criteria from the relational tables (eventually / if needed)
     *
     * @param bookTitleWildcard
     * @return
     */
    public static List<AuthorEntity> findAuthorsByBookTitle(final String bookTitleWildcard) {
        // We have two ways of achieving this:
        
        // (i) select distinct with inner join
        return finishLoad(JOOQ_TX.get()
            .selectDistinct(AUTHOR.fields())
            .select(AUTHOR_EXTRA.fields())
            .from(AUTHOR)
            .leftJoin(AUTHOR_EXTRA)
            .on(AUTHOR_EXTRA.ID.eq(AUTHOR.ID))
            .innerJoin(BOOK)
            .on(AUTHOR.ID.eq(BOOK.AUTHOR_ID))
            .where(BOOK.NAME.like(bookTitleWildcard))
            .fetch()
            .intoMap(AUTHOR.ID));
        // (ii) select with exists sub query
        //        return finishLoad(
        //            JOOQ_TX.get()
        //                .select()
        //                .from(AUTHOR)
        //                .leftJoin(AUTHOR_EXTRA)
        //                .on(AUTHOR_EXTRA.ID.eq(AUTHOR.ID))
        //                .whereExists(
        //                    JOOQ_TX.get()
        //                        .select()
        //                        .from(BOOK)
        //                        .where(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
        //                        .and(BOOK.NAME.like(bookTitleWildcard))
        //                )
        //                .fetch()
        //                .intoMap(AUTHOR.ID)
        //        );
        
        // Both options should give an equivalent result, but we prefer using joins instead of sub queries.
        // Preferably, compare the two queries against your schema and check which is most optimal since
        // there is no general rule of thumb as to which is faster, and whatever indices you have will
        // affect the query performance.
    }
    
    public static List<AuthorEntity> find(final String authorName, final String bookName) {
        return finishLoad(JOOQ_TX.get()
            .selectDistinct(AUTHOR.fields())
            .select(AUTHOR_EXTRA.fields())
            .from(AUTHOR)
            .leftJoin(AUTHOR_EXTRA)
            .on(AUTHOR_EXTRA.ID.eq(AUTHOR.ID))
            .innerJoin(BOOK)
            .on(AUTHOR.ID.eq(BOOK.AUTHOR_ID))
            .where(AUTHOR.NAME.like("%" + authorName + "%"), BOOK.NAME.like("%" + bookName + "%"))
            .limit(100)
            .fetch()
            .intoMap(AUTHOR.ID));
    }
    
    private static AuthorEntity finishLoad(final Record record) {
        final AuthorRecord author = record.into(AuthorRecord.class);
        return new AuthorEntity(author,
            record.get(AUTHOR_EXTRA.ID) != null ? record.into(AuthorExtraRecord.class) : null,
            JOOQ_TX.get().fetch(BOOK, BOOK.AUTHOR_ID.eq(author.getId())),
            JOOQ_TX.get().fetch(ARTICLE, ARTICLE.AUTHOR_ID.eq(author.getId())),
            JOOQ_TX.get()
                .select(ARTICLE_RELATED.fields())
                .from(ARTICLE_RELATED.join(ARTICLE).on(ARTICLE_RELATED.ARTICLE_ID.eq(ARTICLE.ID)))
                .where(ARTICLE.AUTHOR_ID.in(author.getId()))
                .fetchGroups(ARTICLE_RELATED.ARTICLE_ID, ArticleRelatedRecord.class));
    }
    
    private static List<AuthorEntity> finishLoad(final Map<Long, Record> map) {
        final Set<Long> ids = map.keySet();
        
        final Map<Long, Result<BookRecord>> books = JOOQ_TX.get()
            .selectFrom(BOOK)
            .where(BOOK.AUTHOR_ID.in(ids))
            .fetchGroups(BOOK.AUTHOR_ID);
        
        final Map<Long, Result<ArticleRecord>> articles = JOOQ_TX.get()
            .selectFrom(ARTICLE)
            .where(ARTICLE.AUTHOR_ID.in(ids))
            .fetchGroups(ARTICLE.AUTHOR_ID);
        
        final Map<Long, Map<Long, List<ArticleRelatedRecord>>> articleRelated = JOOQ_TX.get()
            .select(ARTICLE.AUTHOR_ID)
            .select(ARTICLE_RELATED.fields())
            .from(ARTICLE_RELATED.join(ARTICLE).on(ARTICLE_RELATED.ARTICLE_ID.eq(ARTICLE.ID)))
            .where(ARTICLE.AUTHOR_ID.in(ids))
            .fetchGroups(ARTICLE.AUTHOR_ID)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().intoGroups(ARTICLE_RELATED.ARTICLE_ID, ArticleRelatedRecord.class)));
        
        return map.values()
            .stream()
            .map(record -> {
                final AuthorRecord author = record.into(AuthorRecord.class);
                return new AuthorEntity(author,
                    record.get(AUTHOR_EXTRA.ID) != null ? record.into(AuthorExtraRecord.class) : null,
                    books.get(author.getId()),
                    articles.get(author.getId()),
                    Optional.ofNullable(articleRelated.get(author.getId())).orElse(Collections.emptyMap()));
            })
            .collect(Collectors.toList());
    }
    
    private AuthorsRepository() {}
    
}

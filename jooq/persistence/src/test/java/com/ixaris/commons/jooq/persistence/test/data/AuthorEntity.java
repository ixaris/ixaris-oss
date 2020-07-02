package com.ixaris.commons.jooq.persistence.test.data;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Article.ARTICLE;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.ArticleRelated.ARTICLE_RELATED;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Book.BOOK;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableBiMap;

import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.jooq.persistence.EntityMap;
import com.ixaris.commons.jooq.persistence.JooqHelper;
import com.ixaris.commons.jooq.persistence.OptionalRecord;
import com.ixaris.commons.jooq.persistence.RecordMap;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Article;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Author;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Nationality;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRelatedRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.AuthorExtraRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.AuthorRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.BookRecord;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.persistence.lib.mapper.EnumCodeMapper;

public final class AuthorEntity extends Entity<AuthorEntity> {
    
    public static final EnumCodeMapper<Nationality, Character> NATIONALITY_MAPPING = EnumCodeMapper.with(ImmutableBiMap.of(Nationality.ALIEN,
        'A',
        Nationality.MARTIAN,
        'M',
        Nationality.EARTHLING,
        'E'));
    
    private final AuthorRecord author;
    private final OptionalRecord<AuthorExtraRecord> extra;
    private final RecordMap<String, BookRecord> books;
    private final EntityMap<String, ArticleEntity> articles;
    
    private AuthorEntity(final AuthorRecord author) {
        this.author = author;
        this.extra = OptionalRecord.withNewRecordSupplier(() -> {
            final AuthorExtraRecord record = new AuthorExtraRecord();
            record.setId(author.getId());
            return record;
        });
        this.books = RecordMap.withNewRecordFunction(key -> new BookRecord(UniqueIdGenerator.generate(), author.getId(), key));
        this.articles = EntityMap.withNewEntityFunction(key -> {
            final ArticleRecord article = new ArticleRecord();
            article.setId(UniqueIdGenerator.generate());
            article.setAuthorId(author.getId());
            article.setName(key);
            return new ArticleEntity(article);
        });
    }
    
    public AuthorEntity(final Author author) {
        this(new AuthorRecord());
        this.author.setId(author.getId());
        update(author);
    }
    
    AuthorEntity(final AuthorRecord author,
                 final AuthorExtraRecord extra,
                 final Collection<BookRecord> books,
                 final Collection<ArticleRecord> articles,
                 final Map<Long, ? extends Collection<ArticleRelatedRecord>> articleRelated) {
        this(author);
        this.extra.fromExisting(extra);
        this.books.fromExisting(books, BookRecord::getName);
        this.articles.fromExisting(articles,
            article -> new ArticleEntity(article, articleRelated.get(article.getId())),
            e -> e.article.getName());
    }
    
    public AuthorRecord getAuthor() {
        return author;
    }
    
    public OptionalRecord<AuthorExtraRecord> getExtra() {
        return extra;
    }
    
    public RecordMap<String, BookRecord> getBooks() {
        return books;
    }
    
    public EntityMap<String, ArticleEntity> getArticles() {
        return articles;
    }
    
    public AuthorEntity apply(final Consumer<AuthorRecord> consumer) {
        if (consumer != null) {
            consumer.accept(author);
        }
        return this;
    }
    
    @Override
    public AuthorEntity store() {
        attachAndStore(author);
        extra.store();
        books.store();
        articles.store();
        return this;
    }
    
    @Override
    public AuthorEntity delete() {
        // deleting individual child collections looks lke (order is important. parent table deleted after)
        // books.delete();
        // articles.delete();
        
        if (!books.isEmpty()) {
            JOOQ_TX.get().delete(BOOK).where(BOOK.AUTHOR_ID.equal(author.getId())).execute();
            books.afterDelete();
        }
        if (!articles.isEmpty()) {
            JooqHelper
                .delete(ARTICLE_RELATED)
                .from(ARTICLE_RELATED.join(ARTICLE).on(ARTICLE_RELATED.ARTICLE_ID.eq(ARTICLE.ID)))
                .where(ARTICLE.AUTHOR_ID.equal(author.getId()))
                .execute();
            JOOQ_TX.get().delete(ARTICLE).where(ARTICLE.AUTHOR_ID.equal(author.getId())).execute();
            articles.afterDelete();
        }
        extra.delete();
        attachAndDelete(author);
        return this;
    }
    
    public Author toProtobuf() {
        return Author.newBuilder()
            .setId(author.getId())
            .setOwnerId(author.getOwnerId())
            .setName(author.getName())
            .setNationality(NATIONALITY_MAPPING.resolve(author.getNationality()))
            .addAllBooks(books.mapValues(BookRecord::getName))
            .addAllArticles(articles.mapValues(article -> Article.newBuilder()
                .setName(article.article.getName())
                .setPublishedDate(article.article.getPublishedDate())
                .addAllRelated(article.related.mapValues(ArticleRelatedRecord::getRelated))
                .build()))
            .build();
    }
    
    public AuthorEntity update(final Author author) {
        if (this.author.getId() != author.getId()) {
            throw new IllegalStateException("Merging author [" + author + "] into entity with id [" + author.getId() + "]");
        }
        
        this.author.setOwnerId(author.getOwnerId());
        this.author.setName(author.getName());
        this.author.setNationality(NATIONALITY_MAPPING.codify(author.getNationality()));
        
        books.syncWith(author.getBooksList());
        
        articles.syncWith(author.getArticlesList(), Article::getName, (articleEntity, article) -> {
            articleEntity.article.setPublishedDate(article.getPublishedDate());
            articleEntity.related.syncWith(article.getRelatedList());
        });
        
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
    
}

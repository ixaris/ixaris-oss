package com.ixaris.commons.jooq.persistence.test;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.AuthorTestConfiguration.UNIT_NAME;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Article.ARTICLE;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.ArticleRelated.ARTICLE_RELATED;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Author.AUTHOR;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Book.BOOK;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.jooq.persistence.test.AuthorAssert.AuthorCreateAssert;
import com.ixaris.commons.jooq.persistence.test.AuthorAssert.OptionalAuthorCreateAssert;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Article;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Author;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Nationality;
import com.ixaris.commons.jooq.persistence.test.data.AuthorEntity;
import com.ixaris.commons.jooq.persistence.test.data.AuthorsRepository;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.SyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.exception.DuplicateEntryException;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { AuthorTestConfiguration.class })
@DirtiesContext
public class AuthorSyncTest {
    
    @Autowired
    private SyncPersistenceProvider persistenceProvider;
    
    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("spring.application.name", UNIT_NAME);
    }
    
    @Before
    public void clean() throws Throwable {
        cleanTable(TestTenants.DEFAULT);
    }
    
    private List<Article> getNArticles(final int n) {
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            articles.add(
                Article.newBuilder().setName("Article" + i).setPublishedDate(UniqueIdGenerator.generate()).build());
        }
        return articles;
    }
    
    @Test
    public void createAuthor_authorCreatedAsExpected() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final List<String> booksList = Arrays.asList("Fifty shades of pink", "Cloud Atlas");
            
            final List<Article> articleList = getNArticles(3);
            final AuthorEntity entity = persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .addAllArticles(articleList)
                    .build())
                .store());
            
            new AuthorCreateAssert(entity)
                .hasId(id)
                .hasNationality(Nationality.EARTHLING)
                .hasName(TestTenants.DEFAULT)
                .hasBooks(booksList)
                .hasArticles(articleList);
            
            final AuthorEntity entity2 = persistenceProvider.transaction(() -> AuthorsRepository.lookup(id));
            new AuthorCreateAssert(entity2).hasId(id).hasBooks(booksList).hasArticles(articleList);
        });
    }
    
    @Test
    public void createAuthor_authorCreatedAndLookupAsExpected() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final List<String> booksList = Arrays.asList("Inferno", "Deception Point");
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store());
            
            // needs to wait prior insert completion
            final AuthorEntity entity = persistenceProvider.transaction(() -> AuthorsRepository.lookup(id));
            new AuthorCreateAssert(entity)
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasBooks(booksList)
                .hasNationality(Nationality.EARTHLING);
        });
    }
    
    @Test
    public void lookupAbsentAuthor_nullAuthor_failed() throws Throwable {
        withAsyncLocals(() -> Assertions.assertThatThrownBy(() -> persistenceProvider.transaction(() -> AuthorsRepository.lookup(-1L)))
            .isInstanceOf(EntityNotFoundException.class));
    }
    
    @Test
    public void crudAuthor_insertLookupUpdateEnum() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder().setId(id).setName(TestTenants.DEFAULT).setNationality(Nationality.EARTHLING).build())
                .store());
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING);
            
            persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.MARTIAN)
                        .build())
                .store());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.MARTIAN);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndAddToRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            final long id = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store());
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            final List<String> updatedBooksList = new ArrayList<>(booksList);
            updatedBooksList.add("Two shades of pink");
            persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(updatedBooksList)
                        .build())
                .store());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(updatedBooksList);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveFromRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            final long id = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store());
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            final List<String> updatedBooksList = booksList.subList(0, 1);
            persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(updatedBooksList)
                        .build())
                .store());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(updatedBooksList);
        });
    }
    
    @Test
    public void crudAuthor_batchinsert_duplicateId() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            
            Assertions.assertThatThrownBy(() -> {
                persistenceProvider.transaction(() -> {
                    final List<AuthorEntity> authors = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        final long id = UniqueIdGenerator.generate();
                        final AuthorEntity authorEntity = AuthorsRepository.fromProtobuf(
                            Author.newBuilder()
                                .setId(id)
                                .setName(TestTenants.DEFAULT)
                                .setNationality(Nationality.EARTHLING)
                                .addAllBooks(booksList)
                                .build());
                        authorEntity.store();
                        authors.add(authorEntity);
                    }
                    
                    final AuthorEntity duplicateAuthorEntity = AuthorsRepository.fromProtobuf(
                        Author.newBuilder()
                            .setId(authors.get(0).getAuthor().getId())
                            .setName(TestTenants.DEFAULT + "duplicateId")
                            .setNationality(Nationality.MARTIAN)
                            .addAllBooks(booksList)
                            .build());
                    duplicateAuthorEntity.store();
                    return authors;
                });
            }).isInstanceOf(DuplicateEntryException.class);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveAllEntriesFromRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            final long id = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store());
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            // send no books
            persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .build())
                .store());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasNoBooks();
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveArticles() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final List<Article> articleList = getNArticles(3);
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .addAllArticles(articleList)
                    .build())
                .store());
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList)
                .hasArticles(articleList);
            
            final List<Article> updatedArticlesList = articleList.subList(0, 2);
            persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(booksList)
                        .addAllArticles(updatedArticlesList)
                        .build())
                .store());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList)
                .hasArticles(updatedArticlesList);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupDelete() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder().setId(id).setName(TestTenants.DEFAULT).setNationality(Nationality.EARTHLING).build())
                .store());
            
            final Optional<AuthorEntity> opEntity = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING);
            
            persistenceProvider.transaction(() -> AuthorsRepository.lookup(id).delete());
            
            final Optional<AuthorEntity> opEntity2 = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            new OptionalAuthorCreateAssert(opEntity2).isNotPresent();
        });
    }
    
    @Test
    public void fetchAll_fetchAsExpected() throws Throwable {
        createTenAuthors(100L, TestTenants.DEFAULT);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> authors = persistenceProvider.transaction(() -> AuthorsRepository.findAll(0, 25));
            Assertions.assertThat(authors.size()).isEqualTo(10);
            authors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(100L, 109L));
        });
    }
    
    @Test
    public void findAll_withPaging_findsCurrentPage() throws Throwable {
        createTenAuthors(200L, TestTenants.DEFAULT);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> authors = persistenceProvider.transaction(() -> AuthorsRepository.findAll(3, 5));
            Assertions.assertThat(authors.size()).isEqualTo(5);
            authors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(203L, 207L));
        });
    }
    
    private void withAsyncLocals(final RunnableThrows<? extends Exception> runnable) throws Throwable {
        withAsyncLocals(TestTenants.DEFAULT, runnable);
    }
    
    private void withAsyncLocals(final String tenant, final RunnableThrows<? extends Exception> runnable) throws Throwable {
        AsyncLocal.with(DATA_UNIT, UNIT_NAME).with(TENANT, tenant).exec(runnable);
    }
    
    private void createTenAuthors(final Long start, final String tenant) throws Throwable {
        for (long i = start; i < start + 10L; i++) {
            final long ii = i;
            withAsyncLocals(tenant, () -> {
                persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                    Author.newBuilder()
                        .setId(ii)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.MARTIAN)
                        .build())
                    .store());
            });
        }
    }
    
    private void cleanTable(final String... tenants) throws Throwable {
        for (final String tenant : tenants) {
            withAsyncLocals(tenant, () -> persistenceProvider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(BOOK).execute();
                JOOQ_TX.get().deleteFrom(ARTICLE_RELATED).execute();
                JOOQ_TX.get().deleteFrom(ARTICLE).execute();
                JOOQ_TX.get().deleteFrom(AUTHOR).execute();
                return null;
            }));
        }
    }
    
}

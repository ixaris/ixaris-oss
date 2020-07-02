package com.ixaris.commons.jooq.persistence.test;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.AuthorTestConfiguration.UNIT_NAME;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Article.ARTICLE;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.ArticleRelated.ARTICLE_RELATED;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Author.AUTHOR;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.Book.BOOK;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

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

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.test.CompletionStageAssert;
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
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;
import com.ixaris.commons.persistence.lib.exception.DuplicateEntryException;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

/**
 * Stuff not up to date with: ownerId concept in Author
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { AuthorTestConfiguration.class })
@DirtiesContext
public class AuthorAsyncTest {
    
    @Autowired
    private AsyncPersistenceProvider persistenceProvider;
    
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
            articles.add(Article.newBuilder().setName("Article" + i).setPublishedDate(UniqueIdGenerator.generate()).build());
        }
        return articles;
    }
    
    @Test
    public void createAuthor_authorCreatedAsExpected() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final List<String> booksList = Arrays.asList("Fifty shades of pink", "Cloud Atlas");
            
            final List<Article> articleList = getNArticles(3);
            final CompletionStage<AuthorEntity> exCompletionStage = persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
                .setId(id)
                .setName(TestTenants.DEFAULT)
                .setNationality(Nationality.EARTHLING)
                .addAllBooks(booksList)
                .addAllArticles(articleList)
                .build())
                .store());
            
            CompletionStageAssert
                .assertThat(exCompletionStage)
                .await()
                .isFulfilled(AuthorCreateAssert::new)
                .hasId(id)
                .hasNationality(Nationality.EARTHLING)
                .hasName(TestTenants.DEFAULT)
                .hasBooks(booksList)
                .hasArticles(articleList);
            
            final CompletionStage<AuthorEntity> exCompletionStage2 = persistenceProvider.transaction(() -> AuthorsRepository.lookup(id));
            CompletionStageAssert
                .assertThat(exCompletionStage2)
                .await()
                .isFulfilled(AuthorCreateAssert::new)
                .hasId(id)
                .hasBooks(booksList)
                .hasArticles(articleList);
        });
    }
    
    @Test
    public void createAuthor_authorCreatedAndLookupAsExpected() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final List<String> booksList = Arrays.asList("Inferno", "Deception Point");
            final CompletionStage<AuthorEntity> exCompletionStage = persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
                .setId(id)
                .setName(TestTenants.DEFAULT)
                .setNationality(Nationality.EARTHLING)
                .addAllBooks(booksList)
                .build())
                .store());
            CompletionStageAssert.assertThat(exCompletionStage).await();
            // needs to wait prior insert completion
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.lookup(id)))
                .await()
                .isFulfilled(AuthorCreateAssert::new)
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasBooks(booksList)
                .hasNationality(Nationality.EARTHLING);
        });
    }
    
    @Test
    public void lookupAbsentAuthor_nullAuthor_failed() throws Throwable {
        withAsyncLocals(() -> CompletionStageAssert
            .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.lookup(-1L)))
            .await()
            .isRejectedWith(EntityNotFoundException.class));
    }
    
    @Test
    public void crudAuthor_insertLookupUpdateEnum() throws Throwable {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder().setId(id).setName(TestTenants.DEFAULT).setNationality(Nationality.EARTHLING).build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING);
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.MARTIAN)
                        .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.MARTIAN);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndAddToRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Some title 1", "Some title 2");
            final long id = UniqueIdGenerator.generate();
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            final List<String> updatedBooksList = new ArrayList<>(booksList);
            updatedBooksList.add("Some title 3");
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(updatedBooksList)
                        .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(updatedBooksList);
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.findAuthorsByBookTitle("Some title%")))
                .await()
                .isFulfilled()
                .satisfies(l -> {
                    Assertions.assertThat(l).hasSize(1);
                    new AuthorAssert(l.get(0))
                        .hasId(id)
                        .hasName(TestTenants.DEFAULT)
                        .hasNationality(Nationality.EARTHLING)
                        .hasBooks(updatedBooksList);
                });
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveFromRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            final long id = UniqueIdGenerator.generate();
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            final List<String> updatedBooksList = booksList.subList(0, 1);
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(updatedBooksList)
                        .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
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
            
            final CompletionStage<List<AuthorEntity>> exCompletionStageCreate = persistenceProvider.transaction(() -> {
                final List<AuthorEntity> authors = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    final long id = UniqueIdGenerator.generate();
                    final AuthorEntity authorEntity = AuthorsRepository.fromProtobuf(Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(booksList)
                        .build());
                    authorEntity.store();
                    authors.add(authorEntity);
                }
                
                final AuthorEntity duplicateAuthorEntity = AuthorsRepository.fromProtobuf(Author.newBuilder()
                    .setId(authors.get(0).getAuthor().getId())
                    .setName(TestTenants.DEFAULT + "duplicateId")
                    .setNationality(Nationality.MARTIAN)
                    .addAllBooks(booksList)
                    .build());
                duplicateAuthorEntity.store();
                return authors;
            });
            
            CompletionStageAssert.assertThat(exCompletionStageCreate).await().isRejectedWith(DuplicateEntryException.class);
        });
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveAllEntriesFromRelationalTable() throws Throwable {
        withAsyncLocals(() -> {
            final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
            final long id = UniqueIdGenerator.generate();
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList);
            
            // send no books
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
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
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(id)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.EARTHLING)
                    .addAllBooks(booksList)
                    .addAllArticles(articleList)
                    .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING)
                .hasBooks(booksList)
                .hasArticles(articleList);
            
            final List<Article> updatedArticlesList = articleList.subList(0, 2);
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository
                .lookup(id)
                .update(
                    Author.newBuilder()
                        .setId(id)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.EARTHLING)
                        .addAllBooks(booksList)
                        .addAllArticles(updatedArticlesList)
                        .build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
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
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder().setId(id).setName(TestTenants.DEFAULT).setNationality(Nationality.EARTHLING).build())
                .store()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isPresent()
                .hasId(id)
                .hasName(TestTenants.DEFAULT)
                .hasNationality(Nationality.EARTHLING);
            
            assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.lookup(id).delete()));
            
            CompletionStageAssert
                .assertThat(persistenceProvider.transaction(() -> AuthorsRepository.fetch(id)))
                .await()
                .isFulfilled(OptionalAuthorCreateAssert::new)
                .isNotPresent();
        });
    }
    
    @Test
    public void fetchAll_fetchAsExpected() throws Throwable {
        createTenAuthors(100L, TestTenants.DEFAULT);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> authors = block(persistenceProvider.transaction(() -> AuthorsRepository.findAll(0, 25)));
            Assertions.assertThat(authors.size()).isEqualTo(10);
            authors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(100L, 109L));
        });
    }
    
    @Test
    public void findAll_withPaging_findsCurrentPage() throws Throwable {
        createTenAuthors(200L, TestTenants.DEFAULT);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> authors = block(persistenceProvider.transaction(() -> AuthorsRepository.findAll(3, 5)));
            Assertions.assertThat(authors.size()).isEqualTo(5);
            authors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(203L, 207L));
        });
    }
    
    @Test
    public void transactionRequired_multipleNestedTransactions_secondTransactionFails_shouldRollbackFirstTransaction() throws Throwable {
        withAsyncLocals(() -> {
            final long authorId = UniqueIdGenerator.generate();
            try {
                block(persistenceProvider.transactionRequired(() -> {
                    block(persistenceProvider.transactionRequired(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
                        .setId(authorId)
                        .setName(TestTenants.DEFAULT)
                        .setNationality(Nationality.MARTIAN)
                        .build())
                        .store()));
                    
                    throw new IllegalStateException();
                }));
            } catch (IllegalStateException e) {
                CompletionStageAssert
                    .assertThat(persistenceProvider.transactionRequired(() -> AuthorsRepository.fetch(authorId)))
                    .await()
                    .isFulfilled()
                    .satisfies(authorEntity -> Assertions.assertThat(authorEntity).isEmpty());
            }
        });
    }
    
    private void withAsyncLocals(final RunnableThrows<? extends Exception> runnable) throws Throwable {
        withAsyncLocals(TestTenants.DEFAULT, runnable);
    }
    
    private void withAsyncLocals(final String tenant, final RunnableThrows<? extends Exception> runnable) throws Throwable {
        AsyncLocal.with(TENANT, tenant).exec(runnable);
    }
    
    private void createTenAuthors(final Long start, final String tenant) throws Throwable {
        for (long i = start; i < start + 10L; i++) {
            final long ii = i;
            withAsyncLocals(tenant, () -> assertFulfilled(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(
                Author.newBuilder()
                    .setId(ii)
                    .setName(TestTenants.DEFAULT)
                    .setNationality(Nationality.MARTIAN)
                    .build())
                .store())));
        }
    }
    
    private void cleanTable(final String... tenants) throws Throwable {
        for (final String tenant : tenants) {
            withAsyncLocals(tenant, () -> block(persistenceProvider.transaction(() -> {
                JOOQ_TX.get().deleteFrom(BOOK).execute();
                JOOQ_TX.get().deleteFrom(ARTICLE_RELATED).execute();
                JOOQ_TX.get().deleteFrom(ARTICLE).execute();
                JOOQ_TX.get().deleteFrom(AUTHOR).execute();
                return null;
            })));
        }
    }
    
    private <T> void assertFulfilled(final Async<T> async) {
        CompletionStageAssert.assertThat(async).await().isFulfilled();
    }
    
}

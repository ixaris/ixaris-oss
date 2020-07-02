package com.ixaris.commons.jooq.persistence.test;

import static com.ixaris.commons.jooq.persistence.test.AuthorAssert.assertThat;
import static com.ixaris.commons.jooq.persistence.test.AuthorTestConfiguration.UNIT_NAME;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import com.google.common.collect.ImmutableList;

import com.ixaris.commons.jooq.persistence.JooqSyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Article;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Author;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Nationality;
import com.ixaris.commons.jooq.persistence.test.data.AuthorEntity;
import com.ixaris.commons.jooq.persistence.test.data.AuthorsRepository;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.exception.EntityNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { AuthorTestConfiguration.class })
@DirtiesContext
public class AuthorTest {
    
    @Autowired
    private JooqSyncPersistenceProvider persistenceProvider;
    
    @Before
    public void clean() throws Throwable {
        cleanTable();
    }
    
    private static final String NAME = "Giacondo";
    
    private static final String OTHER_NAME = "Philis";
    
    @BeforeClass
    public static void setUp() {
        System.setProperty("spring.application.name", UNIT_NAME);
    }
    
    private List<Article> getNArticles(final int n) {
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            articles.add(Article.newBuilder()
                .setName("A" + UniqueIdGenerator.generate())
                .setPublishedDate(UniqueIdGenerator.generate())
                .addAllRelated(Arrays.asList("rel1", "rel2"))
                .build());
        }
        return articles;
    }
    
    @Test
    public void createAuthor_authorCreatedAndLookup_ReturnedAsExpected() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final List<String> booksList = Arrays.asList("Fifty shades of pink", "Cloud Atlas");
        final List<Article> articleList = getNArticles(3);
        
        final AuthorEntity authorEntity = withAsyncLocals(() -> persistenceProvider.transaction(() -> {
            final AuthorEntity author = AuthorsRepository.fromProtobuf(Author.newBuilder()
                .setId(id)
                .setOwnerId(ownerId)
                .setName(NAME)
                .setNationality(Nationality.EARTHLING)
                .addAllBooks(booksList)
                .addAllArticles(articleList)
                .build());
            author.getBooks().update("TEST");
            author.getBooks().remove("TEST");
            author.getArticles().update(articleList, Article::getName, null);
            author.store();
            return author;
        }));
        
        assertThat(authorEntity)
            .hasId(id)
            .hasOwnerId(ownerId)
            .hasNationality(Nationality.EARTHLING)
            .hasName(NAME)
            .hasBooks(booksList)
            .hasArticles(articleList);
        
        final AuthorEntity a = withAsyncLocals(() -> persistenceProvider.transaction(() -> AuthorsRepository.lookup(id)));
        assertThat(a).hasId(id).hasOwnerId(ownerId).hasBooks(booksList).hasArticles(articleList);
    }
    
    @Test
    public void createAuthor_russianAuthorCreatedAndLookup_ReturnedAsExpected() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final String russian = new String("у́сский".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        
        final AuthorEntity authorEntity = withAsyncLocals(() -> persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
            .setId(id)
            .setOwnerId(ownerId)
            .setName(russian)
            .setNationality(Nationality.EARTHLING)
            .build())
            .store()));
        
        assertThat(authorEntity).hasId(id).hasOwnerId(ownerId).hasNationality(Nationality.EARTHLING).hasName("у́сский");
        
        final AuthorEntity a = withAsyncLocals(() -> persistenceProvider.transaction(() -> AuthorsRepository.lookup(id)));
        assertThat(a).hasId(id).hasOwnerId(ownerId).hasName("у́сский");
    }
    
    @Test
    public void lookupAbsentAuthor_nullAuthor_failed() {
        withAsyncLocals(() -> {
            Assertions
                .assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> persistenceProvider.transaction(() -> AuthorsRepository.lookup(-1L)));
        });
    }
    
    @Test
    public void crudAuthor_UpdateEnum() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), Collections.emptyList());
        updateFetchAndAssert(id, ownerId, NAME, Nationality.MARTIAN, Collections.emptyList(), Collections.emptyList());
    }
    
    @Test
    public void crudAuthor_UpdateName() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), Collections.emptyList());
        updateFetchAndAssert(id, ownerId, OTHER_NAME, Nationality.EARTHLING, Collections.emptyList(), Collections.emptyList());
    }
    
    @Test
    public void crudAuthor_insertLookupAndAddToRelationalTable() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
        
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, booksList, Collections.emptyList());
        
        final List<String> updatedBooksList = new ArrayList<>(booksList);
        updatedBooksList.add("Two shades of pink");
        
        updateFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, updatedBooksList, Collections.emptyList());
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveFromRelationalTable() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
        
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, booksList, Collections.emptyList());
        
        final List<String> updatedBooksList = booksList.subList(0, 1);
        
        updateFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, updatedBooksList, Collections.emptyList());
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveAllEntriesFromRelationalTable() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final List<String> booksList = Arrays.asList("Fifty shades of red", "Fifty shades of blue");
        
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, booksList, Collections.emptyList());
        updateFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), Collections.emptyList());
    }
    
    @Test
    public void crudAuthor_insertLookupAndRemoveArticles() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        final List<Article> articleList = getNArticles(3);
        
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), articleList);
        
        final List<Article> updatedArticlesList = articleList.subList(0, 2);
        updateFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), updatedArticlesList);
    }
    
    @Test
    public void crudAuthor_insertLookupDelete() {
        final long id = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        
        createFetchAndAssert(id, ownerId, NAME, Nationality.EARTHLING, Collections.emptyList(), Collections.emptyList());
        
        withAsyncLocals(() -> {
            persistenceProvider.transaction(() -> AuthorsRepository.lookup(id).delete());
            
            final Optional<AuthorEntity> optionalAuthor = persistenceProvider.transaction(() -> AuthorsRepository.fetch(id));
            OptionalAuthorAssert.assertify(optionalAuthor).isNotPresent();
        });
    }
    
    @Test
    public void duplicateDelete_throwsEntityNotFoundException() {
        withAsyncLocals(() -> {
            final long id = UniqueIdGenerator.generate();
            final long ownerId = UniqueIdGenerator.generate();
            persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
                .setId(id)
                .setOwnerId(ownerId)
                .setName(NAME)
                .setNationality(Nationality.MARTIAN)
                .build())
                .store());
            
            persistenceProvider.transaction(() -> AuthorsRepository.lookup(id).delete());
            Assertions
                .assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> persistenceProvider.transaction(() -> AuthorsRepository.lookup(id).delete()));
        });
    }
    
    @Test
    public void fetchAll_fetchAsExpected() throws Throwable {
        createTenAuthors(100L);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> allAuthors = persistenceProvider.transaction(() -> AuthorsRepository.findAll(0, 10));
            Assertions.assertThat(allAuthors.size()).isEqualTo(10);
            allAuthors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(100L, 109L));
        });
    }
    
    @Test
    public void findAll_withPaging_findsCurrentPage() throws Throwable {
        createTenAuthors(200L);
        withAsyncLocals(() -> {
            final Collection<AuthorEntity> allAuthors = persistenceProvider.transaction(() -> AuthorsRepository.findAll(3, 5));
            Assertions.assertThat(allAuthors.size()).isEqualTo(5);
            allAuthors.forEach(author -> Assertions.assertThat(author.getAuthor().getId()).isBetween(203L, 207L));
        });
    }
    
    @Test
    public void findAuthorsByBookTitleWildcard_CorrectAuthorsShouldBeReturned() throws Throwable {
        final long firstAuthorId = UniqueIdGenerator.generate();
        final long secondAuthorId = UniqueIdGenerator.generate();
        final long thirdAuthorId = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        
        createFetchAndAssert(firstAuthorId,
            ownerId,
            "JRR Tolkien",
            Nationality.EARTHLING,
            ImmutableList.of("Lord of the Rings: A", "Lord of the Rings: B", "Lord of the Rings: C", "The Hobbit"),
            Collections.emptyList());
        createFetchAndAssert(secondAuthorId,
            ownerId,
            "George RR Martin",
            Nationality.EARTHLING,
            ImmutableList.of("Game of Thrones", "A Storm of Swords"),
            Collections.emptyList());
        createFetchAndAssert(thirdAuthorId,
            ownerId,
            "John",
            Nationality.EARTHLING,
            ImmutableList.of("Lord of the Rings: D"),
            Collections.emptyList());
        
        withAsyncLocals(() -> {
            final List<AuthorEntity> authors = persistenceProvider.transaction(() -> AuthorsRepository.findAuthorsByBookTitle("Lord of the Rings%"));
            Assertions.assertThat(authors).extracting(a -> a.getAuthor().getName()).containsExactly("JRR Tolkien", "John");
        });
    }
    
    @Test
    public void findAuthorsUsingQueryAdapter_CorrectAuthorsShouldBeReturned() throws Throwable {
        final long firstAuthorId = UniqueIdGenerator.generate();
        final long secondAuthorId = UniqueIdGenerator.generate();
        final long thirdAuthorId = UniqueIdGenerator.generate();
        final long ownerId = UniqueIdGenerator.generate();
        
        createFetchAndAssert(firstAuthorId,
            ownerId,
            "JRR Tolkien",
            Nationality.EARTHLING,
            ImmutableList.of("Lord of the Rings: A", "Lord of the Rings: B", "Lord of the Rings: C", "The Hobbit"),
            Collections.emptyList());
        createFetchAndAssert(secondAuthorId,
            ownerId,
            "George RR Martin",
            Nationality.EARTHLING,
            ImmutableList.of("Game of Thrones", "A Storm of Swords"),
            Collections.emptyList());
        createFetchAndAssert(thirdAuthorId,
            ownerId,
            "John",
            Nationality.EARTHLING,
            ImmutableList.of("Lord of the Rings: D"),
            Collections.emptyList());
        
        withAsyncLocals(() -> {
            final List<AuthorEntity> authors = persistenceProvider.transaction(() -> AuthorsRepository.find("JRR", "Lord of the Rings"));
            Assertions.assertThat(authors).extracting(a -> a.getAuthor().getName()).containsExactly("JRR Tolkien");
        });
    }
    
    private <E extends Exception> void withAsyncLocals(final RunnableThrows<E> runnable) throws E {
        TENANT.exec(TestTenants.DEFAULT, runnable);
    }
    
    private <T> T withAsyncLocals(final CallableThrows<T, RuntimeException> runnable) {
        return TENANT.exec(TestTenants.DEFAULT, runnable);
    }
    
    private AuthorEntity createFetchAndAssert(final long authorId,
                                              final long ownerId,
                                              final String name,
                                              final Nationality nationality,
                                              final List<String> booksList,
                                              final List<Article> articlesList) {
        return withAsyncLocals(() -> {
            final Optional<AuthorEntity> optionalAuthorEntity = Optional.of(persistenceProvider.transaction(() -> AuthorsRepository.fromProtobuf(Author.newBuilder()
                .setId(authorId)
                .setOwnerId(ownerId)
                .setName(name)
                .setNationality(nationality)
                .addAllBooks(booksList)
                .addAllArticles(articlesList)
                .build())
                .store()));
            
            OptionalAuthorAssert
                .assertify(optionalAuthorEntity)
                .isPresent()
                .hasId(authorId)
                .hasOwnerId(ownerId)
                .hasName(name)
                .hasNationality(nationality)
                .hasBooks(booksList)
                .hasArticles(articlesList);
            
            return optionalAuthorEntity.get();
        });
    }
    
    private AuthorEntity updateFetchAndAssert(final long authorId,
                                              final long ownerId,
                                              final String name,
                                              final Nationality nationality,
                                              final List<String> booksList,
                                              final List<Article> articlesList) {
        return withAsyncLocals(() -> {
            final Optional<AuthorEntity> optionalAuthorEntity = persistenceProvider.transaction(() -> AuthorsRepository
                .fetch(authorId)
                .map(a -> a
                    .update(Author.newBuilder()
                        .setId(authorId)
                        .setOwnerId(ownerId)
                        .setName(name)
                        .setNationality(nationality)
                        .addAllBooks(booksList)
                        .addAllArticles(articlesList)
                        .build())
                    .store()));
            
            OptionalAuthorAssert
                .assertify(optionalAuthorEntity)
                .isPresent()
                .hasId(authorId)
                .hasOwnerId(ownerId)
                .hasName(name)
                .hasNationality(nationality)
                .hasBooks(booksList)
                .hasArticles(articlesList);
            
            return optionalAuthorEntity.get();
        });
    }
    
    private void createTenAuthors(final long start) {
        final long ownerId = UniqueIdGenerator.generate();
        withAsyncLocals(() -> persistenceProvider.transaction(() -> {
            for (long i = start; i < start + 10L; i++) {
                AuthorsRepository.fromProtobuf(Author.newBuilder()
                    .setId(i)
                    .setOwnerId(ownerId)
                    .setName(NAME)
                    .setNationality(Nationality.MARTIAN)
                    .build())
                    .store();
            }
            return null;
        }));
    }
    
    private void cleanTable() throws Throwable {
        withAsyncLocals(() -> persistenceProvider.transaction(() -> {
            AuthorsRepository.findAll(0, 400).forEach(AuthorEntity::delete);
            return null;
        }));
    }
    
}

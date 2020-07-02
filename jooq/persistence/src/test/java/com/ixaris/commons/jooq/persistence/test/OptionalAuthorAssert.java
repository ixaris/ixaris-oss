package com.ixaris.commons.jooq.persistence.test;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Article;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Author;
import com.ixaris.commons.jooq.persistence.test.PersistenceTest.Nationality;
import com.ixaris.commons.jooq.persistence.test.data.AuthorEntity;

/**
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 */
public class OptionalAuthorAssert extends ObjectAssert<Optional<Author>> {
    
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private OptionalAuthorAssert(final Optional<AuthorEntity> actual) {
        super(actual.map(entity -> Optional.of(entity.toProtobuf())).orElse(Optional.empty()));
    }
    
    public OptionalAuthorAssert isPresent() {
        Assertions.assertThat(actual.isPresent());
        return this;
    }
    
    public static OptionalAuthorAssert assertify(final Optional<AuthorEntity> actual) {
        return new OptionalAuthorAssert(actual);
    }
    
    public OptionalAuthorAssert isNotPresent() {
        Assertions.assertThat(!actual.isPresent());
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasName(String name) {
        Assertions.assertThat(actual.get().getName()).isEqualTo(name);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasId(long id) {
        Assertions.assertThat(actual.get().getId()).isEqualTo(id);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasOwnerId(long ownerId) {
        Assertions.assertThat(actual.get().getOwnerId()).isEqualTo(ownerId);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasNationality(Nationality nationality) {
        Assertions.assertThat(actual.get().getNationality() == nationality);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasBooks(final Iterable<String> books) {
        Assertions.assertThat(actual.get().getBooksList()).containsOnlyElementsOf(books);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasNoBooks() {
        Assertions.assertThat(actual.get().getBooksList().isEmpty()).isTrue();
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasArticles(final Iterable<Article> articles) {
        Assertions.assertThat(actual.get().getArticlesList()).containsOnlyElementsOf(articles);
        return this;
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OptionalAuthorAssert hasNoArticles() {
        Assertions.assertThat(actual.get().getArticlesList().isEmpty()).isTrue();
        return this;
    }
    
}

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
class AuthorAssert extends ObjectAssert<Author> {
    
    AuthorAssert(final Author actual) {
        super(actual);
    }
    
    AuthorAssert(final AuthorEntity actual) {
        super(actual.toProtobuf());
    }
    
    static AuthorAssert assertThat(final Author actual) {
        return new AuthorAssert(actual);
    }
    
    static AuthorAssert assertThat(final AuthorEntity actual) {
        return new AuthorAssert(actual.toProtobuf());
    }
    
    AuthorAssert hasName(final String name) {
        Assertions.assertThat(actual.getName()).isEqualTo(name);
        return this;
    }
    
    AuthorAssert hasId(final long id) {
        Assertions.assertThat(actual.getId()).isEqualTo(id);
        return this;
    }
    
    AuthorAssert hasOwnerId(final long ownerId) {
        Assertions.assertThat(actual.getOwnerId()).isEqualTo(ownerId);
        return this;
    }
    
    AuthorAssert hasNationality(final Nationality nationality) {
        Assertions.assertThat(actual.getNationality()).isEqualTo(nationality);
        return this;
    }
    
    AuthorAssert hasBooks(final Iterable<String> books) {
        Assertions.assertThat(actual.getBooksList()).containsOnlyElementsOf(books);
        return this;
    }
    
    AuthorAssert hasArticles(final Iterable<Article> articles) {
        Assertions.assertThat(actual.getArticlesList()).containsOnlyElementsOf(articles);
        return this;
    }
    
    static class AuthorCreateAssert extends ObjectAssert<Author> {
        
        AuthorCreateAssert(final Author actual) {
            super(actual);
        }
        
        AuthorCreateAssert(final AuthorEntity actual) {
            super(actual.toProtobuf());
        }
        
        AuthorCreateAssert hasName(final String name) {
            Assertions.assertThat(actual.getName()).isEqualTo(name);
            return this;
        }
        
        AuthorCreateAssert hasId(final long id) {
            Assertions.assertThat(actual.getId()).isEqualTo(id);
            return this;
        }
        
        AuthorCreateAssert hasNationality(final Nationality nationality) {
            Assertions.assertThat(actual.getNationality()).isEqualTo(nationality);
            return this;
        }
        
        AuthorCreateAssert hasBooks(final Iterable<String> books) {
            Assertions.assertThat(actual.getBooksList()).containsOnlyElementsOf(books);
            return this;
        }
        
        AuthorCreateAssert hasArticles(final Iterable<Article> articles) {
            Assertions.assertThat(actual.getArticlesList()).containsOnlyElementsOf(articles);
            return this;
        }
    }
    
    static class OptionalAuthorCreateAssert extends ObjectAssert<Optional<Author>> {
        
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        OptionalAuthorCreateAssert(final Optional<AuthorEntity> actual) {
            super(actual.map(AuthorEntity::toProtobuf));
        }
        
        OptionalAuthorCreateAssert isPresent() {
            Assertions.assertThat(actual.isPresent()).isTrue();
            return this;
        }
        
        OptionalAuthorCreateAssert isNotPresent() {
            Assertions.assertThat(actual.isPresent()).isFalse();
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasName(final String name) {
            Assertions.assertThat(actual.get().getName()).isEqualTo(name);
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasId(final long id) {
            Assertions.assertThat(actual.get().getId()).isEqualTo(id);
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasNationality(final Nationality nationality) {
            Assertions.assertThat(actual.get().getNationality()).isEqualTo(nationality);
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasBooks(final Iterable<String> books) {
            Assertions.assertThat(actual.get().getBooksList()).containsOnlyElementsOf(books);
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasNoBooks() {
            Assertions.assertThat(actual.get().getBooksList()).isEmpty();
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasArticles(final Iterable<Article> articles) {
            Assertions.assertThat(actual.get().getArticlesList()).containsOnlyElementsOf(articles);
            return this;
        }
        
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        OptionalAuthorCreateAssert hasNoArticles() {
            Assertions.assertThat(actual.get().getArticlesList()).isEmpty();
            return this;
        }
        
    }
    
}

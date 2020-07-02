package com.ixaris.commons.jooq.persistence.test.data;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.jooq.persistence.test.jooq.tables.ArticleRelated.ARTICLE_RELATED;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.jooq.persistence.RecordMap;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRecord;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.ArticleRelatedRecord;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;

public final class ArticleEntity extends Entity<ArticleEntity> {
    
    final ArticleRecord article;
    final RecordMap<String, ArticleRelatedRecord> related;
    
    public ArticleEntity(final ArticleRecord article) {
        this.article = article;
        this.related = RecordMap.withMapSupplierAndNewRecordFunction(LinkedHashMap::new,
            key -> new ArticleRelatedRecord(UniqueIdGenerator.generate(), article.getId(), key));
    }
    
    ArticleEntity(final ArticleRecord article, final Collection<ArticleRelatedRecord> articleRelated) {
        this(article);
        if (articleRelated != null) {
            this.related.fromExisting(articleRelated, ArticleRelatedRecord::getRelated);
        }
    }
    
    public ArticleRecord getArticle() {
        return article;
    }
    
    public RecordMap<String, ArticleRelatedRecord> getRelated() {
        return related;
    }
    
    public ArticleEntity apply(final Consumer<ArticleRecord> consumer) {
        if (consumer != null) {
            consumer.accept(article);
        }
        return this;
    }
    
    @Override
    public ArticleEntity store() {
        attachAndStore(article);
        related.store();
        return this;
    }
    
    @Override
    public ArticleEntity delete() {
        // deleting individual child records
        // related.delete();
        
        // deleting by article id
        if (!related.isEmpty()) {
            JOOQ_TX.get().delete(ARTICLE_RELATED).where(ARTICLE_RELATED.ARTICLE_ID.equal(article.getId())).execute();
            related.afterDelete();
        }
        
        attachAndDelete(article);
        return this;
    }
}

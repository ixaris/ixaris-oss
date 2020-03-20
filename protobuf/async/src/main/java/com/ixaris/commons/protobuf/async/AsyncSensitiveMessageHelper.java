package com.ixaris.commons.protobuf.async;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.applyMappers;
import static com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.collectSensitiveFields;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.Message.Builder;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataCollection;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;

/**
 * Helper class for messages with sensitive fields that may need to be mapped to some other value e.g.
 * masking/tokenisation
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class AsyncSensitiveMessageHelper {
    
    private AsyncSensitiveMessageHelper() {}
    
    /**
     * Apply mappings on sensitive data where the mapping is resolved as a promise
     *
     * @param builder The message builder to transform
     * @param temporaryDataMapper the mapping function of the values
     * @param permanentDataMapper the mapping function of the values
     * @return Updated builder with the mapper applied to all sensitive fields
     */
    public static Async<Builder> mapSensitiveFields(final Builder builder,
                                                    final AsyncSensitiveDataFunction temporaryDataMapper,
                                                    final AsyncSensitiveDataFunction permanentDataMapper) {
        return mapSensitiveFields(builder, temporaryDataMapper, permanentDataMapper, Async::result);
    }
    
    /**
     * Apply mappings on sensitive data where the mapping is resolved as a promise
     *
     * @param builder The message builder to transform
     * @param temporaryDataMapper the mapping function of the values
     * @param permanentDataMapper the mapping function of the values
     * @param maskedDataMapper the mapping function of the values that need to be masked
     * @return Updated builder with the mapper applied to all sensitive fields
     */
    public static Async<Builder> mapSensitiveFields(final Builder builder,
                                                    final AsyncSensitiveDataFunction temporaryDataMapper,
                                                    final AsyncSensitiveDataFunction permanentDataMapper,
                                                    final AsyncSensitiveDataFunction maskedDataMapper) {
        final SensitiveDataCollection sensitiveDataCollection = new SensitiveDataCollection();
        collectSensitiveFields(sensitiveDataCollection, builder, new HashSet<>());
        
        final CompletableFuture<List<SensitiveDataContext>> temporaryDataFuture = temporaryDataMapper
            .apply(sensitiveDataCollection.getTemporaryData())
            .toCompletableFuture();
        final CompletableFuture<List<SensitiveDataContext>> permanentDataFuture = permanentDataMapper
            .apply(sensitiveDataCollection.getPermanentData())
            .toCompletableFuture();
        final CompletableFuture<List<SensitiveDataContext>> maskedDataFuture = maskedDataMapper
            .apply(sensitiveDataCollection.getMaskedData())
            .toCompletableFuture();
        
        return all(temporaryDataFuture, permanentDataFuture, maskedDataFuture)
            .map(results -> applyMappers(builder, sensitiveDataCollection, results.get1(), results.get2(), results.get3()));
    }
    
}

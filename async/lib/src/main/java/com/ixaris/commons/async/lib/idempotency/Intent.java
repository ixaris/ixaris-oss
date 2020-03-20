package com.ixaris.commons.async.lib.idempotency;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CommonsAsyncLib.AsyncLocalValue;
import com.ixaris.commons.async.lib.CommonsAsyncLib.IntentValue;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.ToStringUtil;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * An intent representing some action done within the service.
 *
 * <p>An intent is represented by
 *
 * <ul>
 *   <li>a unique id assigned to the intent of performing a compound process
 *   <li>a path that identifies an activity within the process; should be similar in nature to method + url in HTTP, i.e. represents the resource
 *       and action, such that the same action on 2 different items has a different path
 *   <li>a hash, or fingerprint, of the payload, used as a last resort in case a process needs to perform the same operation on the same instance
 *       with different payloads
 * </ul>
 *
 * The only scenario which is ambiguous is performing the same operation on the same path with the same payload twice legitimately, however, so
 * far we have not found a realistic use case for this (retrying is not one of them, as that use case actually wants idempotency semantics)
 */
@SuppressWarnings("squid:S1700")
public final class Intent {
    
    public static final String KEY_INTENT_ID = "INTENT_ID";
    
    public static final AsyncLocal<Intent> INTENT = new AsyncLocal<Intent>("intent", true) {
        
        @Override
        public AsyncLocalValue encode(final Intent value) {
            return AsyncLocalValue
                .newBuilder()
                .setBytesValue(IntentValue.newBuilder().setId(value.id).setPath(value.path).setHash(value.hash).build().toByteString())
                .build();
        }
        
        @Override
        public Intent decode(final AsyncLocalValue value) throws InvalidProtocolBufferException {
            final IntentValue intent = MessageHelper.parse(IntentValue.class, value.getBytesValue());
            return new Intent(intent.getId(), intent.getPath(), intent.getHash());
        }
        
    };
    
    private final long id;
    private final String path;
    private final long hash;
    
    public Intent(final long id, final String path, final long hash) {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        this.id = id;
        this.path = path;
        this.hash = hash;
    }
    
    public long getId() {
        return id;
    }
    
    public String getPath() {
        return path;
    }
    
    public long getHash() {
        return hash;
    }
    
    /**
     * concatenate the path, to be used when a process forks locally, to keep idempotency for each fork
     */
    public Intent concat(final String extra) {
        return new Intent(id, path + extra, hash);
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (id == other.id) && path.equals(other.path) && (hash == other.hash));
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("id", id).with("path", path).with("hash", hash).toString();
    }
    
}

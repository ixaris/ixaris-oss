package com.ixaris.commons.microservices.lib.common.exception;

import java.util.Map.Entry;

import com.google.protobuf.ByteString;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ServerTimeout;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.SuspendedTasks;
import com.ixaris.commons.protobuf.lib.MessageHelper;

public final class ServerTimeoutException extends ServiceException {
    
    public static ServerTimeoutException merge(final ServerTimeoutException e1, final ServerTimeoutException e2) {
        return new ServerTimeoutException((e1.serverTimeout != null)
            ? (e2.serverTimeout == null) ? e1.serverTimeout : merge(e1.serverTimeout, e2.serverTimeout)
            : e2.serverTimeout);
    }
    
    private static ServerTimeout merge(final ServerTimeout e1, final ServerTimeout e2) {
        final ServerTimeout.Builder builder = e1.toBuilder();
        for (final Entry<String, SuspendedTasks> entry : e2.getServiceTasksMap().entrySet()) {
            builder
                .getServiceTasksMap()
                .compute(entry.getKey(), (k, v) -> {
                    if (v == null) {
                        return entry.getValue();
                    } else {
                        return v.toBuilder().addAllTask(entry.getValue().getTaskList()).build();
                    }
                });
        }
        return builder.build();
    }
    
    private final ServerTimeout serverTimeout;
    
    public ServerTimeoutException() {
        super(ResponseStatusCode.SERVER_TIMEOUT, null);
        this.serverTimeout = null;
    }
    
    public ServerTimeoutException(final ServerTimeout serverTimeout) {
        this(MessageHelper.json(serverTimeout), serverTimeout);
    }
    
    public ServerTimeoutException(final String statusMessage) {
        super(ResponseStatusCode.SERVER_TIMEOUT, statusMessage);
        this.serverTimeout = null;
    }
    
    public ServerTimeoutException(final Throwable cause) {
        this(cause.getMessage(), cause);
    }
    
    public ServerTimeoutException(final String statusMessage, final Throwable cause) {
        super(ResponseStatusCode.SERVER_TIMEOUT, statusMessage, cause);
        this.serverTimeout = null;
    }
    
    public ServerTimeoutException(final String statusMessage, final ServerTimeout serverTimeout) {
        super(ResponseStatusCode.SERVER_TIMEOUT, statusMessage);
        this.serverTimeout = serverTimeout;
    }
    
    public ServerTimeoutException(final String statusMessage, final ServerTimeout serverTimeout, final Throwable cause) {
        super(ResponseStatusCode.SERVER_TIMEOUT, statusMessage, cause);
        this.serverTimeout = serverTimeout;
    }
    
    @Override
    public ByteString getPayload(final boolean json) {
        return serverTimeout != null ? MessageHelper.bytes(serverTimeout, json) : null;
    }
    
    public ServerTimeout getServerTimeout() {
        return serverTimeout;
    }
}

package com.ixaris.commons.microservices.web.logging;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.protobuf.lib.MessageHelper;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;

/**
 * A factory that accepts messages and returns a sanitised version of the same message. Use this to prevent sensitive
 * data from leaking to logs.
 *
 * @author benjie.gatt
 */
public class SanitisedLogFactory {
    
    public String getSanitisedMessage(final MessageLite message) {
        if (message == null) {
            return "";
        }
        
        if (message instanceof Message) {
            return transformSensitiveMessage((Message) message);
        } else {
            return MessageHelper.json(message);
        }
    }
    
    private String transformSensitiveMessage(final Message message) {
        final Builder builder = SensitiveMessageHelper.mapSensitiveFields(
            message.toBuilder(),
            this::transformSensitiveData,
            this::transformSensitiveData,
            this::transformSensitiveData);
        return MessageHelper.json(builder.build());
    }
    
    private List<SensitiveDataContext> transformSensitiveData(final List<SensitiveDataContext> sensitiveData) {
        final List<SensitiveDataContext> transformed = new ArrayList<>(sensitiveData.size());
        for (final SensitiveDataContext temporaryDatum : sensitiveData) {
            transformed.add(temporaryDatum.transformSensitiveData("*****"));
        }
        return transformed;
    }
    
    public String getSanitisedResponsePayload(final LoggingResponse response) {
        final String sanitisedPayload;
        if (response.getResponse() != null) {
            sanitisedPayload = getSanitisedMessage(response.getResponse());
        } else if (response.getSanitisedPayload() != null) {
            sanitisedPayload = response.getSanitisedPayload();
        } else {
            sanitisedPayload = "";
        }
        return sanitisedPayload;
    }
}

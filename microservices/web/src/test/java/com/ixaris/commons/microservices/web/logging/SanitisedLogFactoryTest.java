package com.ixaris.commons.microservices.web.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.web.logging.TestLogData.DataWithBlankedField;
import com.ixaris.commons.microservices.web.logging.TestLogData.NestedField;
import com.ixaris.commons.protobuf.lib.MessageHelper;

@RunWith(MockitoJUnitRunner.class)
public class SanitisedLogFactoryTest {
    
    private SanitisedLogFactory logFactory;
    
    @Before
    public void setUp() {
        logFactory = new SanitisedLogFactory();
    }
    
    @Test
    public void getSanitisedMessage_sensitiveValuesBlankedOut() {
        final DataWithBlankedField message = DataWithBlankedField.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber("123456789")
            .setMaskedValue("masked")
            .build();
        final String sanitisedPayload = logFactory.getSanitisedMessage(message);
        
        final JsonObject sanitisedJson = new JsonParser().parse(sanitisedPayload).getAsJsonObject();
        
        assertThat(sanitisedJson.get("data").getAsString()).isEqualTo("datavalue");
        assertThat(sanitisedJson.get("dontlogme").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("password").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("cardNumber").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("maskedValue").getAsString()).isEqualTo("*****");
    }
    
    @Test
    public void getSanitisedMessage_nestedData_sensitiveValuesBlankedOut() {
        final DataWithBlankedField message = DataWithBlankedField.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber("123456789")
            .setMaskedValue("masked")
            .setNestedValue(NestedField.newBuilder().setNestedSensitiveData("DontDoThisNeither").build())
            .build();
        final String sanitisedPayload = logFactory.getSanitisedMessage(message);
        
        final JsonObject sanitisedJson = new JsonParser().parse(sanitisedPayload).getAsJsonObject();
        
        assertThat(sanitisedJson.get("data").getAsString()).isEqualTo("datavalue");
        assertThat(sanitisedJson.get("dontlogme").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("password").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("cardNumber").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("maskedValue").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("nestedValue").getAsJsonObject().get("nestedSensitiveData").getAsString()).isEqualTo("*****");
    }
    
    @Test
    public void getSanitisedMessage_withNullMessage_emptyStringReturned() {
        final String sanitisedPayload = logFactory.getSanitisedMessage(null);
        
        assertThat(sanitisedPayload).isEmpty();
    }
    
    @Test
    public void getSanitisedResponse_withPayload_payloadTrustedAndLeftAsIs() {
        final DataWithBlankedField message = DataWithBlankedField.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber("123456789")
            .build();
        final String jsonMessage = MessageHelper.json(message);
        
        final String sanitisedPayload = logFactory.getSanitisedResponsePayload(LoggingResponse.fromSanitisedPayload(jsonMessage,
            ResponseStatusCode.OK));
        
        final JsonObject sanitisedJson = new JsonParser().parse(sanitisedPayload).getAsJsonObject();
        
        assertThat(sanitisedJson.get("data").getAsString()).isEqualTo("datavalue");
        assertThat(sanitisedJson.get("dontlogme").getAsString()).isEqualTo("Dontdoit");
        assertThat(sanitisedJson.get("password").getAsString()).isEqualTo("Noreallydont");
        assertThat(sanitisedJson.get("cardNumber").getAsString()).isEqualTo("123456789");
    }
    
    @Test
    public void getSanitisedResponse_withMessage_sensitiveValuesBlankedOut() {
        final DataWithBlankedField message = DataWithBlankedField.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber("123456789")
            .build();
        
        final String sanitisedPayload = logFactory.getSanitisedResponsePayload(LoggingResponse.fromUnsanitisedMessage(message,
            ResponseStatusCode.OK));
        
        final JsonObject sanitisedJson = new JsonParser().parse(sanitisedPayload).getAsJsonObject();
        assertThat(sanitisedJson.get("data").getAsString()).isEqualTo("datavalue");
        assertThat(sanitisedJson.get("dontlogme").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("password").getAsString()).isEqualTo("*****");
        assertThat(sanitisedJson.get("cardNumber").getAsString()).isEqualTo("*****");
    }
    
    @Test
    public void getSanitisedResponse_withResponseCodeOnly_emptyPayloadReturned() {
        final String sanitisedPayload = logFactory.getSanitisedResponsePayload(LoggingResponse.fromEmptyResponse(ResponseStatusCode.OK));
        
        assertThat(sanitisedPayload).isEmpty();
    }
    
}

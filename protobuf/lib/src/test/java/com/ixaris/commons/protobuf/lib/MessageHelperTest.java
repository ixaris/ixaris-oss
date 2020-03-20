package com.ixaris.commons.protobuf.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.protobuf.lib.example.Example.ExampleMessage;
import com.ixaris.commons.protobuf.lib.example.Example.ExampleMessage.ExampleEnum;
import com.ixaris.commons.protobuf.lib.example.Example.FingerprintMessage;
import com.ixaris.commons.protobuf.lib.example.Example.Nested;
import com.ixaris.commons.protobuf.lib.example.Example.Nested.E;

public class MessageHelperTest {
    
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");
    
    @Test
    public void testFingerprint() {
        final FingerprintMessage m = FingerprintMessage.newBuilder()
            .setS("test1")
            .addL(1L)
            .addL(2L)
            .putM("A", 1)
            .putM("B", 2)
            .setNested(Nested.newBuilder().setE(E.A).build())
            .build();
        
        // this just check s that list, map and enum are handled
        final long mFingerprint = MessageHelper.fingerprint(m);
        
        final FingerprintMessage m2 = FingerprintMessage.newBuilder().setS("").setNested(Nested.newBuilder().setE(E.DEFAULT).build()).build();
        
        final FingerprintMessage m3 = FingerprintMessage.newBuilder().build();
        
        assertEquals(MessageHelper.fingerprint(m2), MessageHelper.fingerprint(m3));
        assertNotEquals(mFingerprint, MessageHelper.fingerprint(m2));
    }
    
    @Test
    public void testFingerprintWithDefaultsinList() {
        final FingerprintMessage m = FingerprintMessage.newBuilder().addL(0L).addL(0L).build();
        
        final FingerprintMessage m2 = FingerprintMessage.newBuilder().build();
        
        assertNotEquals(MessageHelper.fingerprint(m), MessageHelper.fingerprint(m2));
    }
    
    @Test
    public void protobufToJsonAndBack_shouldRemainEqual() throws InvalidProtocolBufferException {
        final ExampleMessage message = ExampleMessage.newBuilder()
            .setBoolean(true)
            .setString("test")
            .setInteger(42)
            .setLong(43L)
            .setEnum(ExampleEnum.ONE)
            .setNested(Nested.newBuilder().setC(56).setD(1024L).setE(E.A).build())
            .build();
        
        final String actualJson = MessageHelper.json(message, true);
        final ExampleMessage parsedMessage = MessageHelper.parse(ExampleMessage.getDefaultInstance(), actualJson);
        
        Assertions.assertThat(parsedMessage).isEqualTo(message);
    }
    
    @Test
    public void protobufToJsonConversion_NoDefaultFields_AllFieldsSerialized() {
        final ExampleMessage message = ExampleMessage.newBuilder()
            .setBoolean(true)
            .setString("test")
            .setInteger(42)
            .setLong(43L)
            .setEnum(ExampleEnum.ONE)
            .setNested(Nested.newBuilder().setC(56).setD(1024L).setE(E.A).build())
            .build();
        
        final String expectedJson =
            "{\"boolean\":true,\"string\":\"test\",\"integer\":42,\"long\":\"43\",\"enum\":\"ONE\",\"nested\":{\"a\":\"\",\"b\":\"\",\"c\":56,\"d\":\"1024\",\"e\":\"A\"}}";
        
        final String actualJson = MessageHelper.json(message);
        final String cleaned = cleanWhitespaces(actualJson);
        Assertions.assertThat(cleaned).isEqualToIgnoringWhitespace(expectedJson);
    }
    
    @Test
    public void protobufToJsonConversion_AllDefaultFields_AllFieldsSerializedWithDefaultValues() {
        final ExampleMessage message = ExampleMessage.newBuilder().build();
        final String expectedJson = "{\"boolean\":false,\"string\":\"\",\"integer\":0,\"long\":\"0\",\"enum\":\"ZERO\"}";
        
        final String actualJson = MessageHelper.json(message);
        final String cleaned = cleanWhitespaces(actualJson);
        Assertions.assertThat(cleaned).isEqualToIgnoringWhitespace(expectedJson);
    }
    
    @Test
    public void jsonToProtobufConversion_ValidMessageAsJson_RespectiveProtobufMessageReturned() throws InvalidProtocolBufferException {
        final String json = "{\"boolean\":true,\"string\":\"test\",\"integer\":42,\"long\":\"43\",\"enum\":\"ONE\",\"nested\":{\"c\":56,\"d\":\"1024\",\"e\":\"A\"}}";
        final ExampleMessage expectedMessage = ExampleMessage.newBuilder()
            .setBoolean(true)
            .setString("test")
            .setInteger(42)
            .setLong(43L)
            .setEnum(ExampleEnum.ONE)
            .setNested(Nested.newBuilder().setC(56).setD(1024L).setE(E.A).build())
            .build();
        
        Assertions.assertThat(MessageHelper.parse(ExampleMessage.getDefaultInstance(), json)).isEqualTo(expectedMessage);
    }
    
    @Test
    public void jsonToProtobufConversion_EmptyJsonMessage_DefaultProtobufMessageReturned() throws InvalidProtocolBufferException {
        final String json = "{}";
        final ExampleMessage expectedMessage = ExampleMessage.newBuilder().build();
        Assertions.assertThat(MessageHelper.parse(ExampleMessage.getDefaultInstance(), json)).isEqualTo(expectedMessage);
    }
    
    private static String cleanWhitespaces(final String toClean) {
        return WHITESPACES.matcher(toClean).replaceAll("");
    }
}

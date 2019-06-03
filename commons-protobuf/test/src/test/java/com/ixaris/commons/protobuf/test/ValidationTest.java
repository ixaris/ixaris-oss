package com.ixaris.commons.protobuf.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.ixaris.commons.protobuf.lib.MessageValidator;
import com.ixaris.commons.protobuf.test.example.Example;
import com.ixaris.commons.protobuf.test.example.Example.Nested;
import com.ixaris.commons.protobuf.test.example.Example.TestMessage.SomeOtherMessage;
import org.junit.jupiter.api.Test;
import valid.Valid.FieldValidation.Type;
import valid.Valid.MessageValidation;

public class ValidationTest {
    
    @Test
    public void testValidation() {
        final MessageValidation validation = MessageValidator.validate(
            Example.TestMessage.newBuilder()
                .setStr("AAA")
                .putIntMap("", 2)
                .putIntMap("a", 3)
                .setNested(Nested.newBuilder().setA("A"))
                .setSomeOtherMessage(SomeOtherMessage.newBuilder().setStr("AAA"))
                .build()
        );
        assertThat(validation.getInvalid()).isTrue();
        assertThat(validation.getFieldsList().get(0).getName()).isEqualTo("int_map[");
        assertThat(validation.getFieldsList().get(0).getErrorsList().get(0).getType()).isEqualTo(Type.REQUIRED);
    }
    
}

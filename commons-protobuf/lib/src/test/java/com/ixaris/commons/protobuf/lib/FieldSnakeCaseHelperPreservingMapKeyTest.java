package com.ixaris.commons.protobuf.lib;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class FieldSnakeCaseHelperPreservingMapKeyTest {
    
    @Test
    public void snakeToCamel() {
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey("some.other"))
            .isEqualTo("some.other");
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey("some_word.other_word"))
            .isEqualTo("someWord.otherWord");
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey("some_word[some_word]"))
            .isEqualTo("someWord[some_word]");
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey("some_word[some_word"))
            .isEqualTo("someWord[some_word");
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey(
                "some_word[some_word].some_other_word[some_other_word]"
            ))
            .isEqualTo("someWord[some_word].someOtherWord[some_other_word]");
        Assertions
            .assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey(
                "first_field[a_b__c_d].second_field.other_field[some_key"
            ))
            .isEqualTo("firstField[a_b__c_d].secondField.otherField[some_key");
        Assertions.assertThat(MessageValidator.fieldSnakeToCamelCasePreservingMapKey("some._")).isEqualTo("some._");
    }
    
}

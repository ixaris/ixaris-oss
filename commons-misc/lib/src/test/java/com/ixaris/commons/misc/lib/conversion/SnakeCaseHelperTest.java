package com.ixaris.commons.misc.lib.conversion;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SnakeCaseHelperTest {
    
    @Test
    public void snakeToCamel() {
        Assertions.assertThat(SnakeCaseHelper.snakeToCamelCase("some")).isEqualTo("some");
        Assertions.assertThat(SnakeCaseHelper.snakeToCamelCase("some_word")).isEqualTo("someWord");
        Assertions.assertThat(SnakeCaseHelper.snakeToCamelCase("some__word")).isEqualTo("some_Word");
        Assertions
            .assertThat(SnakeCaseHelper.snakeToCamelCase("some_other_word_with_many_parts"))
            .isEqualTo("someOtherWordWithManyParts");
        Assertions.assertThat(SnakeCaseHelper.snakeToCamelCase("_")).isEqualTo("_");
    }
    
    @Test
    public void camelToSnake() {
        Assertions.assertThat(SnakeCaseHelper.camelToSnakeCase("some")).isEqualTo("some");
        Assertions.assertThat(SnakeCaseHelper.camelToSnakeCase("someWord")).isEqualTo("some_word");
        Assertions.assertThat(SnakeCaseHelper.camelToSnakeCase("some_Word")).isEqualTo("some__word");
        Assertions
            .assertThat(SnakeCaseHelper.camelToSnakeCase("someOtherWordWithManyParts"))
            .isEqualTo("some_other_word_with_many_parts");
        Assertions.assertThat(SnakeCaseHelper.camelToSnakeCase("_")).isEqualTo("_");
    }
    
    @Test
    public void snakeToHuman() {
        String str = "some_other_word";
        Assertions.assertThat(SnakeCaseHelper.snakeToHuman(str)).isEqualTo("Some Other Word");
    }
    
}

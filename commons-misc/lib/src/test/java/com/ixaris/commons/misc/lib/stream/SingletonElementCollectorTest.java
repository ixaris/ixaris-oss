package com.ixaris.commons.misc.lib.stream;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

public class SingletonElementCollectorTest {
    
    @Test
    public void optionallyOne_NoElements_ReturnsEmptyOptional() {
        final List<String> elems = Collections.emptyList();
        final Optional<String> collected = elems.stream().collect(SingletonElementCollector
            .optionallyOneOrThrowIfMore());
        Assertions.assertThat(collected).isEmpty();
    }
    
    @Test
    public void optionallyOne_OneElement_ReturnsOptionalContainingElement() {
        final String singletonElement = "test";
        final List<String> elems = Collections.singletonList(singletonElement);
        final Optional<String> collected = elems.stream().collect(SingletonElementCollector
            .optionallyOneOrThrowIfMore());
        Assertions.assertThat(collected).isNotEmpty().contains(singletonElement);
    }
    
    @Test
    public void optionallyOne_MultipleElements_ThrowsException() {
        final List<String> elems = Lists.newArrayList("test", "ab", "cd");
        Assertions
            .assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> elems.stream().collect(SingletonElementCollector.optionallyOneOrThrowIfMore()));
    }
    
}

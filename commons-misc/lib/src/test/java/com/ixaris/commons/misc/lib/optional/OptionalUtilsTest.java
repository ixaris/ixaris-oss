package com.ixaris.commons.misc.lib.optional;

import java.util.HashSet;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class OptionalUtilsTest {
    
    @Test
    public void ifPresentOrElse_valuePresent_executesPresentConsumer() throws Exception {
        final Optional<Integer> presentOptional = Optional.of(1);
        final HashSet<Integer> integerSet = new HashSet<>(1);
        
        OptionalUtils.ifPresentOrElse(presentOptional, integerSet::add, () -> integerSet.add(42));
        Assertions.assertThat(integerSet).containsExactly(1);
    }
    
    @Test
    public void ifPresentOrElse_valueNotPresent_executesNotPresentRunnable() throws Exception {
        final Optional<Integer> emptyOptional = Optional.empty();
        final HashSet<Integer> integerSet = new HashSet<>(1);
        
        OptionalUtils.ifPresentOrElse(emptyOptional, integerSet::add, () -> integerSet.add(42));
        Assertions.assertThat(integerSet).containsExactly(42);
    }
    
}

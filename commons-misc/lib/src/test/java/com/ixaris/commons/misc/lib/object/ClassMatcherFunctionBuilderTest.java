package com.ixaris.commons.misc.lib.object;

import com.ixaris.commons.misc.lib.function.FunctionThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassMatcherFunctionBuilderTest {
    
    @Test
    @SuppressWarnings("unused")
    public void testSimpleMatching() {
        final ClassMatcherFunctionBuilder<Object, String, RuntimeException>
        functionBuilder = ClassMatcherFunctionBuilder.newBuilder();
        final FunctionThrows<Object, String, RuntimeException> function = functionBuilder
            .when(Integer.class)
            .apply(i -> {
                // The following variable is unused but it will protect against incorrect types of the parameter
                final Integer ii = i;
                return "match integer";
            })
            .when(Long.class)
            .apply(l -> {
                // The following variable is unused but it will protect against incorrect types of the parameter
                final Long ll = l;
                return "match long";
            })
            .otherwise(o -> "no match");
        
        Assertions.assertThat(function.apply(100L)).isEqualTo("match long");
        Assertions.assertThat(function.apply(100)).isEqualTo("match integer");
        Assertions.assertThat(function.apply("ABC")).isEqualTo("no match");
    }
    
    @Test
    @SuppressWarnings("unused")
    public void testSubtypeMatching() {
        final ClassMatcherFunctionBuilder<Object, String, RuntimeException>
        functionBuilder = ClassMatcherFunctionBuilder.newBuilder();
        final FunctionThrows<Object, String, RuntimeException> function = functionBuilder
            .when(Long.class)
            .apply(l -> {
                // The following variable is unused but it will protect against incorrect types of the parameter
                final Long ll = l;
                return "match long";
            })
            .when(Number.class)
            .apply(n -> {
                // The following variable is unused but it will protect against incorrect types of the parameter
                final Number nn = n;
                return "match number";
            })
            .otherwiseThrow(n -> new UnsupportedOperationException());
        
        Assertions.assertThat(function.apply(10L)).isEqualTo("match long");
        Assertions.assertThat(function.apply(100)).isEqualTo("match number");
        Assertions.assertThatThrownBy(() -> function.apply("abc"));
    }
}

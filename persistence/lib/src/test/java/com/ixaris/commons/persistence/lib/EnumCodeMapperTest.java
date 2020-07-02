package com.ixaris.commons.persistence.lib;

import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.persistence.lib.mapper.EnumCodeMapper;

/**
 * @author daniel.grech
 */
public class EnumCodeMapperTest {
    
    private enum TestEnum {
        FIRST,
        SECOND
    }
    
    @Test
    public void mappingUsingName_MappingCorrect() {
        final EnumCodeMapper<TestEnum, String> mapping = EnumCodeMapper.byName(TestEnum.values());
        for (final TestEnum e : TestEnum.values()) {
            Assertions.assertThat(mapping.codify(e)).isEqualTo(e.name());
            Assertions.assertThat(mapping.resolve(e.name())).isEqualTo(e);
        }
    }
    
    @Test
    public void mappingUsingMapper_MappingCorrect() {
        final Function<TestEnum, Integer> function = e -> {
            if (e == TestEnum.FIRST)
                return 1;
            else
                return 2;
        };
        final EnumCodeMapper<TestEnum, Integer> mapping = EnumCodeMapper.usingMapper(TestEnum.values(), function);
        for (final TestEnum e : TestEnum.values()) {
            Assertions.assertThat(mapping.codify(e)).isEqualTo(function.apply(e));
            Assertions.assertThat(mapping.resolve(function.apply(e))).isEqualTo(e);
        }
    }
    
}

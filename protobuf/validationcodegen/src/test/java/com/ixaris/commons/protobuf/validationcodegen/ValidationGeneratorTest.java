package com.ixaris.commons.protobuf.validationcodegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

import com.ixaris.commons.collections.lib.DirectedAcyclicGraph;

/**
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class ValidationGeneratorTest {
    
    @Test
    public void generateSingleMessageValidation_validationsWithComplexRegex_generatedCorrectly() {
        // string str = 1 [(valid.field) =
        // "required size(0, 10)
        // regex(^[a-zA-Z0-9_.*@\\-]*$|^([a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@[a-zA-Z0-9_\\-\\+=&&[^//]]+(\\.[a-zA-Z0-9_\\-\\+=&&[^//]]+)+)$)"];
        final Map<String, FieldValidationInfo> fieldValidations = new HashMap<>();
        final DirectedAcyclicGraph<String> fieldDependencies = new DirectedAcyclicGraph<>();
        final List<String> messageKeyEnums = new ArrayList<>();
        final Map<String, String> fqnProtoEnumToJava = new HashMap<>();
        ValidationGenerator.extractFieldValidationInfoAndDependencies("com.ixaris.commons.protobuf.test",
            "str",
            false,
            false,
            JavaType.STRING,
            "string",
            null,
            null,
            "required size(0, 10) regex(^[a-zA-Z0-9_.*@\\-]*$|^([a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@[a-zA-Z0-9_\\-\\+=&&[^//]]+(\\.[a-zA-Z0-9_\\-\\+=&&[^//]]+)+)$)",
            "",
            "",
            "",
            fieldValidations,
            fieldDependencies,
            messageKeyEnums,
            fqnProtoEnumToJava);
        
        Assertions
            .assertThat(fieldValidations.get("str").validation)
            .isEqualTo(
                "v.field(\"str\", m.getStr()).required().size(0, 10).regex(\"^[a-zA-Z0-9_.*@\\\\-]*$|^([a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@[a-zA-Z0-9_\\\\-\\\\+=&&[^//]]+(\\\\.[a-zA-Z0-9_\\\\-\\\\+=&&[^//]]+)+)$\")");
    }
    
    @Test
    public void generateSingleMessageValidation_validations_generatedCorrectly() {
        // map<string, core.nested.Nested.E> str_map = 4 [(valid.field) = "size(2, 5)", (valid.keys) =
        // "in(core.nested.Nested.E)", (valid.values) = "required"];
        final Map<String, FieldValidationInfo> fieldValidations = new HashMap<>();
        final DirectedAcyclicGraph<String> fieldDependencies = new DirectedAcyclicGraph<>();
        final List<String> messageKeyEnums = new ArrayList<>();
        final Map<String, String> fqnProtoEnumToJava = new HashMap<>();
        fqnProtoEnumToJava.put("com.ixaris.core.nested.Nested.E", "com.ixaris.core.nested.ExampleNested.Nested.E");
        ValidationGenerator.extractFieldValidationInfoAndDependencies("com.ixaris.commons.protobuf.test",
            "str_map",
            true,
            true,
            JavaType.MESSAGE,
            ".com.ixaris.commons.protobuf.test.GeneratedMapEntry",
            () -> JavaType.STRING,
            () -> JavaType.ENUM,
            "size(2, 5)",
            "required",
            "",
            "core.nested.Nested.E",
            fieldValidations,
            fieldDependencies,
            messageKeyEnums,
            fqnProtoEnumToJava);
        
        Assertions
            .assertThat(fieldValidations.get("str_map").validation)
            .isEqualTo("v.mapEnumEnum(\"str_map\", com.ixaris.core.nested.ExampleNested.Nested.E.class, m.getStrMapMap()).size(2, 5).entries((k, i) -> { i.required(); })");
        Assertions
            .assertThat(messageKeyEnums.get(0))
            .isEqualTo("case \"str_map\": return getDescriptorForType(com.ixaris.core.nested.ExampleNested.Nested.E.class)");
    }
}

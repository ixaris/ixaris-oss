package com.ixaris.commons.protobuf.async;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;
import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.protobuf.async.example.Example.DataWithSensitiveFields;
import com.ixaris.commons.protobuf.async.example.Example.DataWithSensitiveFields.ExampleEnum;
import com.ixaris.commons.protobuf.async.example.Example.Nested;
import com.ixaris.commons.protobuf.async.example.Example.NestedSensitive;
import com.ixaris.commons.protobuf.lib.MessageHelper;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sensitive.Sensitive.SensitiveDataLifetime;

public class AsyncMessageSensitiveDataTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(AsyncMessageSensitiveDataTest.class);
    private static final int REPEATED_SIZE = 5;
    
    @Test
    public void helper_asyncMapFields_fieldValuePassedToMapper() throws InterruptedException {
        final MapperAssertion temporaryDataMapper = new MapperAssertion();
        final MapperAssertion permanentDataMapper = new MapperAssertion();
        final DataWithSensitiveFields.Builder builder = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber("123456789")
            .setMaskedOnly("maskedvalue")
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .putStrings("test", "test")
            .putMapNotSensitive("test", Nested.newBuilder().build())
            .addRepeatedNested(
                NestedSensitive.newBuilder().setNotSensitive("test").setAnotherCardNumber("test").build()
            )
            .putMapNested(
                "test", NestedSensitive.newBuilder().setNotSensitive("test").setAnotherCardNumber("test").build()
            );
        
        block(AsyncSensitiveMessageHelper.mapSensitiveFields(builder, temporaryDataMapper, permanentDataMapper));
        
        Assertions.assertThat(temporaryDataMapper.fieldName).containsExactly("dontlogme", "password");
        Assertions
            .assertThat(permanentDataMapper.fieldName)
            .containsExactly("cardNumber", "anotherCardNumber", "anotherCardNumber");
    }
    
    @Test
    public void sensitiveData_lifetimeCanBeRetrieved() {
        final String cardNumber = "123456789";
        final DataWithSensitiveFields message = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber(cardNumber)
            .setMaskedOnly("maskedvalue")
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .build();
        
        final List<FieldDescriptor> fields = message.getDescriptorForType().getFields();
        
        for (final FieldDescriptor fieldDescriptor : fields) {
            SensitiveMessageHelper
                .resolveSensitiveData(fieldDescriptor)
                .ifPresent(sensitiveDataLifetime -> {
                    final Object value = message.getField(fieldDescriptor);
                    switch (fieldDescriptor.getName()) {
                        case "card_number":
                            assertThat(sensitiveDataLifetime).isEqualTo(SensitiveDataLifetime.PERMANENT);
                            assertThat(value).isEqualTo(cardNumber);
                            break;
                        case "password":
                        case "dontlogme":
                            assertThat(sensitiveDataLifetime).isEqualTo(SensitiveDataLifetime.TEMPORARY);
                            break;
                        case "masked_only":
                            assertThat(sensitiveDataLifetime).isEqualTo(SensitiveDataLifetime.MASKED);
                            break;
                        default:
                            fail(String.format(
                                "Field [%s] is not sensitive but treated as sensitive with lifetime [%s]",
                                fieldDescriptor.getName(), sensitiveDataLifetime
                            ));
                            break;
                    }
                });
        }
    }
    
    @Test
    public void sensitiveData_sensitiveMapper() throws Exception {
        final String cardNumber = "123456789";
        final DataWithSensitiveFields message = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("")
            .setPassword("Noreallydont")
            .setCardNumber(cardNumber)
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .build();
        
        final DataWithSensitiveFields.Builder messageBuilderToMap = message.toBuilder();
        block(AsyncSensitiveMessageHelper.mapSensitiveFields(
            messageBuilderToMap, new QualifierMapper("temporary_"), new QualifierMapper("permanent_")
        ));
        
        final DataWithSensitiveFields transformedMessage = messageBuilderToMap.build();
        
        assertThat(transformedMessage.getCardNumber()).isEqualTo("permanent_123456789");
        assertThat(transformedMessage.getPassword()).isEqualTo("temporary_Noreallydont");
        assertThat(transformedMessage.getDontlogme()).isEqualTo("");
        assertThat(transformedMessage.getData()).isEqualTo("datavalue");
        LOG.info("Transformed message: {}", MessageHelper.json(transformedMessage));
    }
    
    @Test
    public void sensitiveDataWithNesting_sensitiveMapper() throws Exception {
        final String cardNumber = "123456789";
        final DataWithSensitiveFields message = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber(cardNumber)
            .setNested(NestedSensitive.newBuilder().setAnotherCardNumber(cardNumber).setNotSensitive("notsensitive"))
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .build();
        
        final DataWithSensitiveFields.Builder messageBuilderToMap = message.toBuilder();
        block(AsyncSensitiveMessageHelper.mapSensitiveFields(
            messageBuilderToMap, new QualifierMapper("temporary_"), new QualifierMapper("permanent_")
        ));
        
        final DataWithSensitiveFields transformedMessage = messageBuilderToMap.build();
        
        assertThat(transformedMessage.getCardNumber()).isEqualTo("permanent_123456789");
        assertThat(transformedMessage.getPassword()).isEqualTo("temporary_Noreallydont");
        assertThat(transformedMessage.getDontlogme()).isEqualTo("temporary_Dontdoit");
        assertThat(transformedMessage.getData()).isEqualTo("datavalue");
        assertThat(transformedMessage.getNested().getAnotherCardNumber()).isEqualTo("permanent_123456789");
        assertThat(transformedMessage.getNested().getNotSensitive()).isEqualTo("notsensitive");
        
        LOG.info("Transformed message: {}", MessageHelper.json(transformedMessage));
    }
    
    @SuppressWarnings("Convert2streamapi")
    @Test
    public void sensitiveDataWithRepeated_sensitiveMapper() throws Exception {
        final String cardNumber = "123456789";
        final List<NestedSensitive> repeatedNested = new ArrayList<>(5);
        
        for (int i = 0; i < 5; i++) {
            repeatedNested.add(
                NestedSensitive.newBuilder().setAnotherCardNumber(cardNumber).setNotSensitive("notsensitive").build()
            );
        }
        
        final DataWithSensitiveFields message = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber(cardNumber)
            .addAllRepeatedNested(repeatedNested)
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .build();
        
        final DataWithSensitiveFields.Builder messageBuilderToMap = message.toBuilder();
        block(AsyncSensitiveMessageHelper.mapSensitiveFields(
            messageBuilderToMap, new QualifierMapper("temporary_"), new QualifierMapper("permanent_")
        ));
        
        final DataWithSensitiveFields transformedMessage = messageBuilderToMap.build();
        
        assertThat(transformedMessage.getCardNumber()).isEqualTo("permanent_123456789");
        assertThat(transformedMessage.getPassword()).isEqualTo("temporary_Noreallydont");
        assertThat(transformedMessage.getDontlogme()).isEqualTo("temporary_Dontdoit");
        assertThat(transformedMessage.getData()).isEqualTo("datavalue");
        
        assertThat(
            transformedMessage
                .getRepeatedNestedList()
                .stream()
                .map(NestedSensitive::getAnotherCardNumber)
                .collect(Collectors.toSet())
        )
            .containsExactly("permanent_123456789");
        assertThat(
            transformedMessage
                .getRepeatedNestedList()
                .stream()
                .map(NestedSensitive::getNotSensitive)
                .collect(Collectors.toSet())
        )
            .containsExactly("notsensitive");
        
        LOG.info("Transformed message: {}", MessageHelper.json(transformedMessage));
    }
    
    @SuppressWarnings("Convert2streamapi")
    @Test
    public void sensitiveDataWithRepeatedNested_Promise_sensitiveMapper() throws Throwable {
        final String cardNumber = "123456789";
        final List<NestedSensitive> repeatedNested = new ArrayList<>(REPEATED_SIZE);
        
        for (int i = 0; i < REPEATED_SIZE; i++) {
            repeatedNested.add(
                NestedSensitive.newBuilder()
                    .setAnotherCardNumber(cardNumber + i)
                    .setNotSensitive("notsensitive" + i)
                    .build()
            );
        }
        
        final DataWithSensitiveFields message = DataWithSensitiveFields.newBuilder()
            .setData("datavalue")
            .setDontlogme("Dontdoit")
            .setPassword("Noreallydont")
            .setCardNumber(cardNumber)
            .setMaskedOnly("somemaskedvalue")
            .setNested(
                NestedSensitive.newBuilder().setNotSensitive("notsensitive").setAnotherCardNumber(cardNumber + 1)
            )
            .addAllRepeatedNested(repeatedNested)
            .addAllActions(Arrays.asList(ExampleEnum.ONE, ExampleEnum.TWO))
            .addAllNumbers(Arrays.asList(1L, 2L, 3L, 4L))
            .build();
        
        final DataWithSensitiveFields.Builder messageBuilderToMap = message.toBuilder();
        
        final Async<Builder> async = AsyncSensitiveMessageHelper.mapSensitiveFields(
            messageBuilderToMap,
            objects -> {
                final FutureAsync<List<SensitiveDataContext>> future = new FutureAsync<>();
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        future.complete(
                            objects.stream()
                                .map(o -> o.transformSensitiveData("temporary"))
                                .collect(Collectors.toList())
                        );
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }).start();
                return future;
            },
            objects -> {
                final FutureAsync<List<SensitiveDataContext>> future = new FutureAsync<>();
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        future.complete(
                            objects.stream()
                                .map(o -> o.transformSensitiveData("permanent"))
                                .collect(Collectors.toList())
                        );
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }).start();
                return future;
            }
        );
        
        final DataWithSensitiveFields transformedMessage = (DataWithSensitiveFields) block(async).build();
        
        assertThat(transformedMessage.getCardNumber()).isEqualTo("permanent");
        assertThat(transformedMessage.getPassword()).isEqualTo("temporary");
        assertThat(transformedMessage.getDontlogme()).isEqualTo("temporary");
        assertThat(transformedMessage.getData()).isEqualTo("datavalue");
        assertThat(transformedMessage.getMaskedOnly()).isEqualTo("somemaskedvalue");
        assertThat(transformedMessage.getNested().getNotSensitive()).isEqualTo("notsensitive");
        assertThat(transformedMessage.getNested().getAnotherCardNumber()).isEqualTo("permanent");
        
        for (int i = 0; i < REPEATED_SIZE; i++) {
            assertThat(transformedMessage.getRepeatedNested(i).getAnotherCardNumber()).isEqualTo("permanent");
            assertThat(transformedMessage.getRepeatedNested(i).getNotSensitive()).isEqualTo("notsensitive" + i);
        }
        
        LOG.info("Transformed message: {}", MessageHelper.json(transformedMessage));
    }
    
    private class MapperAssertion implements AsyncSensitiveDataFunction {
        
        private final List<String> fieldName = new ArrayList<>();
        
        @Override
        public Async<List<SensitiveDataContext>> apply(final List<SensitiveDataContext> sensitiveDataContexts) {
            for (final SensitiveDataContext sensitiveDataContext : sensitiveDataContexts) {
                fieldName.add(sensitiveDataContext.getFieldName());
            }
            return result(sensitiveDataContexts);
        }
    }
    
    private class QualifierMapper implements AsyncSensitiveDataFunction {
        
        private final String qualifier;
        
        private QualifierMapper(final String qualifier) {
            this.qualifier = qualifier;
        }
        
        @Override
        public Async<List<SensitiveDataContext>> apply(final List<SensitiveDataContext> sensitiveDataContexts) {
            return result(
                sensitiveDataContexts.stream()
                    .map(sd -> sd.transformSensitiveData(qualifier + sd.getSensitiveData().toString()))
                    .collect(Collectors.toList())
            );
        }
    }
    
}

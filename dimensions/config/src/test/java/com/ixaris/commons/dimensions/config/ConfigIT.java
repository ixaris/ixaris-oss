package com.ixaris.commons.dimensions.config;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;

import com.ixaris.commons.clustering.lib.extra.LocalCluster;
import com.ixaris.commons.collections.lib.ExtendedCollections;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSet;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDef;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDefs;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetUpdates;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValue;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDef;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDefs;
import com.ixaris.commons.dimensions.config.admin.ConfigAdminHelper;
import com.ixaris.commons.dimensions.config.admin.ConfigAdminProto;
import com.ixaris.commons.dimensions.config.admin.UndefinedPropertyException;
import com.ixaris.commons.dimensions.config.cache.LocalConfigCacheWithClusterInvalidateProvider;
import com.ixaris.commons.dimensions.config.data.ConfigValueEntity;
import com.ixaris.commons.dimensions.config.support.ADimDef;
import com.ixaris.commons.dimensions.config.support.BDimDef;
import com.ixaris.commons.dimensions.config.support.DDimDef;
import com.ixaris.commons.dimensions.config.support.EDimDef;
import com.ixaris.commons.dimensions.config.support.FDimDef;
import com.ixaris.commons.dimensions.config.support.HDimDef;
import com.ixaris.commons.dimensions.config.support.HEnum;
import com.ixaris.commons.dimensions.config.support.PDimDef;
import com.ixaris.commons.dimensions.config.support.Part;
import com.ixaris.commons.dimensions.config.support.TestSet1;
import com.ixaris.commons.dimensions.config.support.TestSet2;
import com.ixaris.commons.dimensions.config.support.TestSet3;
import com.ixaris.commons.dimensions.config.support.TestSet4;
import com.ixaris.commons.dimensions.config.support.TestSet5;
import com.ixaris.commons.dimensions.config.support.TestSet6;
import com.ixaris.commons.dimensions.config.support.TestSet7;
import com.ixaris.commons.dimensions.config.support.TestValue1;
import com.ixaris.commons.dimensions.config.support.TestValue2;
import com.ixaris.commons.dimensions.config.support.TestValue3;
import com.ixaris.commons.dimensions.config.support.TestValue4;
import com.ixaris.commons.dimensions.config.support.TestValue5;
import com.ixaris.commons.dimensions.config.support.TestValue6;
import com.ixaris.commons.dimensions.config.support.TestValue7;
import com.ixaris.commons.dimensions.config.support.TestValue8;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * ContextDef Properties Test
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class ConfigIT {
    
    private static final String UNIT = "unit_config";
    private static JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    
    private static final LocalCluster localCluster = new LocalCluster(Collections.emptySet(), Collections.emptySet());
    
    private static final JooqAsyncPersistenceProvider provider = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(TEST_HELPER
        .getDataSource()));
    private static final LocalConfigCacheWithClusterInvalidateProvider cache = new LocalConfigCacheWithClusterInvalidateProvider(
        TEST_HELPER.getMultiTenancy(), localCluster);
    private static final ConfigHelper config = new ConfigHelper(provider, cache);
    private static final ConfigAdminHelper configAdmin = new ConfigAdminHelper(provider, cache);
    private static final ConfigAdminProto protoAdmin = new ConfigAdminProto(
        config, configAdmin);
    
    @BeforeClass
    public static void setup() {
        cache.startup();
        
        TEST_HELPER.getMultiTenancy().addTenant(TestTenants.DEFAULT);
        
        ValueDefRegistry.getInstance().postConstruct();
        Registry.registerInApplicableRegistries(TestValue1.getInstance());
        Registry.registerInApplicableRegistries(TestValue2.getInstance());
        Registry.registerInApplicableRegistries(TestValue3.getInstance());
        Registry.registerInApplicableRegistries(TestValue4.getInstance());
        Registry.registerInApplicableRegistries(TestValue5.getInstance());
        Registry.registerInApplicableRegistries(TestValue6.getInstance());
        Registry.registerInApplicableRegistries(TestValue7.getInstance());
        
        SetDefRegistry.getInstance().postConstruct();
        Registry.registerInApplicableRegistries(TestSet1.getInstance());
        Registry.registerInApplicableRegistries(TestSet2.getInstance());
        Registry.registerInApplicableRegistries(TestSet3.getInstance());
        Registry.registerInApplicableRegistries(TestSet4.getInstance());
        Registry.registerInApplicableRegistries(TestSet5.getInstance());
        Registry.registerInApplicableRegistries(TestSet6.getInstance());
        Registry.registerInApplicableRegistries(TestSet7.getInstance());
    }
    
    @AfterClass
    public static void teardown() {
        TEST_HELPER.destroy();
        ValueDefRegistry.getInstance().preDestroy();
        SetDefRegistry.getInstance().preDestroy();
        
        cache.shutdown();
    }
    
    @Test
    public void getConfigValueDefs_SeveralValuesRegistered_RegisteredValuesRetrieved() throws Throwable {
        final ConfigValueDefs actual = protoAdmin.getConfigValueDefs();
        
        Assertions
            .assertThat(actual.getValueDefsList())
            .containsExactlyInAnyOrder(TestValue1.getInstance().toProtobuf(),
                TestValue2.getInstance().toProtobuf(),
                TestValue3.getInstance().toProtobuf(),
                TestValue4.getInstance().toProtobuf(),
                TestValue5.getInstance().toProtobuf(),
                TestValue6.getInstance().toProtobuf(),
                TestValue7.getInstance().toProtobuf());
    }
    
    @Test
    public void getConfigSetDefs_SeveralValuesRegistered_RegisteredValuesRetrieved() {
        final ConfigSetDefs actual = protoAdmin.getConfigSetDefs();
        
        Assertions
            .assertThat(actual.getSetDefsList())
            .containsExactlyInAnyOrder(TestSet1.getInstance().toProtobuf(),
                TestSet2.getInstance().toProtobuf(),
                TestSet3.getInstance().toProtobuf(),
                TestSet4.getInstance().toProtobuf(),
                TestSet5.getInstance().toProtobuf(),
                TestSet6.getInstance().toProtobuf(),
                TestSet7.getInstance().toProtobuf());
    }
    
    @Test
    public void getConfigValueDef_InvalidKey_IllegalStateException() {
        Assertions.assertThatIllegalStateException().isThrownBy(() -> protoAdmin.getConfigValueDef("INVALID"));
    }
    
    @Test
    public void getConfigValueDef_ValidKey_RegisteredValueRetrieved() {
        final ConfigValueDef actual = protoAdmin.getConfigValueDef(TestValue3.getInstance().getKey());
        Assertions.assertThat(actual).isEqualTo(TestValue3.getInstance().toProtobuf());
    }
    
    @Test
    public void getConfigSetDef_InvalidKey_IllegalStateException() {
        Assertions.assertThatIllegalStateException().isThrownBy(() -> protoAdmin.getConfigSetDef("INVALID"));
    }
    
    @Test
    public void getConfigSetDef_ValidKey_RegisteredValuesRetrieved() {
        final ConfigSetDef actual = protoAdmin.getConfigSetDef(TestSet2.getInstance().getKey());
        Assertions.assertThat(actual).isEqualTo(TestSet2.getInstance().toProtobuf());
    }
    
    @Test
    public void setAndRetrieveValue_TwoValuesSet_returnsMostSpecific() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // Setup
            try {
                block(protoAdmin.setConfigValue(TestValue1.getInstance().getKey(),
                    ConfigValue.newBuilder()
                        .setContext(Context.newBuilder(TestValue1.getInstance()).add(ADimDef.getInstance().create("VA")).build().toProtobuf())
                        .setValue(CommonsDimensionsConfig.Value.newBuilder().addParts("persisted value for VA").build())
                        .build()));
                
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue1.getInstance())
                    .add(BDimDef.getInstance().create("VB"))
                    .build(),
                    new StringValue("persisted value for VB")));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // Assert
            final StringValue v1 = block(config.getConfigValue(Context.newBuilder(TestValue1.getInstance())
                .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"))
                .build())).orElse(null);
            Assertions.assertThat(new StringValue("persisted value for VA")).isEqualTo(v1);
            
            // Teardown
            block(protoAdmin.removeConfigValue(TestValue1.getInstance().getKey(),
                Context.newBuilder(TestValue1.getInstance()).add(ADimDef.getInstance().create("VA")).build().toProtobuf()));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue1.getInstance()).add(BDimDef.getInstance().create("VB")).build()));
            
            final StringValue v2 = block(config.getConfigValue(Context.newBuilder(TestValue1.getInstance())
                .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"))
                .build())).orElse(null);
            
            Assertions.assertThat(v2).isNull();
        }));
    }
    
    @Test
    public void setAndRetrieveValue_severalHierarchicalValuesSet_getValuesFromDifferentLocationsInTheHierarchy() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // Setup
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue2.getInstance()), new StringValue("default for ROOT")));
                
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue2.getInstance())
                    .add(HDimDef.getInstance().create(HEnum.I1))
                    .build(),
                    new StringValue("persisted value for I1")));
                
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue2.getInstance())
                    .add(HDimDef.getInstance().create(HEnum.I2_1))
                    .build(),
                    new StringValue("persisted value for I2_1")));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // Assertions
            final StringValue v3 = block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I1_1))
                .build())).orElse(null);
            Assertions.assertThat(new StringValue("persisted value for I1")).isEqualTo(v3);
            
            final StringValue v4 = block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I2))
                .build())).orElse(null);
            Assertions.assertThat(new StringValue("default for ROOT")).isEqualTo(v4);
            
            final StringValue v5 = block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I2_1))
                .build())).orElse(null);
            Assertions.assertThat(new StringValue("persisted value for I2_1")).isEqualTo(v5);
            
            final StringValue v6 = block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I2_1_1))
                .build())).orElse(null);
            Assertions.assertThat(new StringValue("persisted value for I2_1")).isEqualTo(v6);
            
            final Map<Context<TestValue2>, StringValue> allValuesByValue = block(config.getAllValuesByValue(TestValue2.getInstance(),
                new StringValue("persisted value for I2_1")));
            
            Assertions.assertThat(allValuesByValue).hasSize(1);
            
            // Teardown
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I1))
                .build()));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue2.getInstance())
                .add(HDimDef.getInstance().create(HEnum.I2_1))
                .build()));
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance()).add(HDimDef.getInstance().create(HEnum.I1)).build()));
            } catch (final IllegalStateException expected) {}
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue2.getInstance())
                    .add(HDimDef.getInstance().create(HEnum.I2_1))
                    .build()));
            } catch (final IllegalStateException expected) {}
        }));
    }
    
    @Test
    public void setAndRetrieveSet0() throws Throwable {
        final Dimension<String> vaDimension = ADimDef.getInstance().create("VA");
        final Dimension<String> vbDimension = BDimDef.getInstance().create("VB");
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // test get common value set available in multiple dimensions
            final Set<StringValue> testSet1DimVA = Collections.singleton(new StringValue("1"));
            final Set<StringValue> testSet1DimVB = Sets.newHashSet(new StringValue("1"), new StringValue("2"));
            try {
                block(protoAdmin.setConfigSet(TestSet1.getInstance().getKey(),
                    ConfigSet.newBuilder()
                        .setContext(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build().toProtobuf())
                        .setSet(Value.setToProtobuf(testSet1DimVA))
                        .build()));
                
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vbDimension).build(), testSet1DimVB));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // test check if value is in context set
            final Set<StringValue> getConfigSetDimVAVB = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            Assertions.assertThat(testSet1DimVA).isEqualTo(getConfigSetDimVAVB);
            
            Assertions
                .assertThat(block(config.isValueInSet(Context.newBuilder(TestSet1.getInstance()).addAll(vaDimension, vbDimension).build(),
                    new StringValue("1"))))
                .isTrue();
            Assertions
                .assertThat(block(config.isValueInSet(Context.newBuilder(TestSet1.getInstance()).addAll(vaDimension, vbDimension).build(),
                    new StringValue("2"))))
                .isFalse();
            
            final Set<StringValue> testSet1DimVA2 = Sets.newHashSet(new StringValue("2"), new StringValue("3"));
            
            // test setting and getting from context set - overriding from above
            try {
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build(), testSet1DimVA2));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> getConfigSetWithOverriddenValues = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            
            Assertions.assertThat(testSet1DimVA2).isEqualTo(getConfigSetWithOverriddenValues);
            
            // test adding in context set
            final Set<StringValue> testSet1DimVA3 = Sets.newHashSet(new StringValue("2"),
                new StringValue("3"),
                new StringValue("5"),
                new StringValue("6"));
            try {
                block(protoAdmin.updateConfigSet(TestSet1.getInstance().getKey(),
                    ConfigSetUpdates.newBuilder()
                        .setContext(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build().toProtobuf())
                        .setAdded(Value.setToProtobuf(ExtendedCollections.build(new HashSet<>(), new StringValue("5"), new StringValue("6"))))
                        .setRemoved(Value.setToProtobuf(Collections.emptySet()))
                        .build()));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s2 = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            Assertions.assertThat(testSet1DimVA3).isEqualTo(s2);
            
            // test update in context set
            final Set<StringValue> testSet1DimVA4 = Sets.newHashSet(new StringValue("2"),
                new StringValue("3"),
                new StringValue("8"),
                new StringValue("6"));
            try {
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build(),
                    new SetUpdates<>(Collections.singleton(new StringValue("8")), Collections.singleton(new StringValue("5")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s3 = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            Assertions.assertThat(testSet1DimVA4).isEqualTo(s3);
            
            // test remove from contextSet
            final Set<StringValue> testSet1DimVA5 = Sets.newHashSet(new StringValue("2"), new StringValue("8"), new StringValue("6"));
            try {
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build(),
                    new SetUpdates<>(Collections.emptySet(), Collections.singleton(new StringValue("3")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s4 = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            Assertions.assertThat(testSet1DimVA5).isEqualTo(s4);
            
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vaDimension).build()));
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet1.getInstance()).add(vbDimension).build()));
            
            final Set<? extends Value> s5 = block(config.getConfigSet(Context.newBuilder(TestSet1.getInstance())
                .addAll(vaDimension, vbDimension)
                .build()));
            Assertions.assertThat((Boolean) s5.isEmpty()).isTrue();
        }));
    }
    
    @Test
    public void setAndRetrieveSet1() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.updateConfigSet(Context.empty(TestSet2.getInstance()),
                    new SetUpdates<>(Collections.singleton(new StringValue("1")), Collections.emptySet())));
                
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet2.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    new SetUpdates<>(ExtendedCollections.build(new HashSet<>(), new StringValue("2"), new StringValue("3")),
                        Collections.emptySet())));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s5 = block(config.getConfigSet(Context.newBuilder(TestSet2.getInstance())
                .add(ADimDef.getInstance().create("VA"))
                .build()));
            final Set<StringValue> ps3 = new HashSet<>(3);
            ps3.add(new StringValue("1"));
            ps3.add(new StringValue("2"));
            ps3.add(new StringValue("3"));
            Assertions.assertThat(ps3).isEqualTo(s5);
            
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet2.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            
            final Set<StringValue> s6 = block(config.getConfigSet(Context.newBuilder(TestSet2.getInstance())
                .add(ADimDef.getInstance().create("VA"))
                .build()));
            
            final Set<StringValue> ps4 = new HashSet<>(3);
            ps4.add(new StringValue("1"));
            Assertions.assertThat(ps4).isEqualTo(s6);
        }));
    }
    
    @Test
    public void setAndRetrieveSet2() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            
            // ************** TEST WITH DEFAULTS ****************
            
            final Set<StringValue> ps5 = new HashSet<>(3);
            try {
                ps5.add(new StringValue("default1"));
                ps5.add(new StringValue("default2"));
                block(configAdmin.setConfigSet(Context.empty(TestSet6.getInstance()), ps5));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // test for default value
            Set<StringValue> s1 = block(config.getConfigSet(Context.newBuilder(TestSet6.getInstance())
                .add(ADimDef.getInstance().create("VA"))
                .build()));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // remove from default set
            ps5.remove(new StringValue("default2"));
            try {
                block(configAdmin.updateConfigSet(Context.empty(TestSet6.getInstance()),
                    new SetUpdates<>(Collections.emptySet(), Collections.singleton(new StringValue("default2")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            s1 = block(config.getConfigSet(Context.empty(TestSet6.getInstance())));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // test delete - back to default value
            block(configAdmin.removeConfigSet(Context.empty(TestSet6.getInstance())));
            
            ps5.remove(new StringValue("default1"));
            s1 = block(config.getConfigSet(Context.empty(TestSet6.getInstance())));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            try {
                ps5.add(new StringValue("default1"));
                ps5.add(new StringValue("default2"));
                block(configAdmin.setConfigSet(Context.empty(TestSet6.getInstance()), ps5));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // update in default set
            ps5.remove(new StringValue("default2"));
            ps5.add(new StringValue("default3"));
            try {
                block(configAdmin.updateConfigSet(Context.empty(TestSet6.getInstance()),
                    new SetUpdates<>(Collections.singleton(new StringValue("default3")), Collections.singleton(new StringValue("default2")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            s1 = block(config.getConfigSet(Context.empty(TestSet6.getInstance())));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // test set context set - hides default value, adds set("1","2")
            try {
                ps5.remove(new StringValue("default1"));
                ps5.remove(new StringValue("default3"));
                ps5.add(new StringValue("1"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    ps5));
                ps5.add(new StringValue("2"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    ps5));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            s1 = block(config.getConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // test add to set
            // ps5 has only "1" in it right now
            ps5.add(new StringValue("3"));
            ps5.add(new StringValue("4"));
            try {
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    new SetUpdates<>(ExtendedCollections.build(new HashSet<>(), new StringValue("3"), new StringValue("4")),
                        Collections.emptySet())));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            s1 = block(config.getConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // test updating in context set
            ps5.remove(new StringValue("1"));
            ps5.add(new StringValue("New"));
            try {
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    new SetUpdates<>(Collections.singleton(new StringValue("New")), Collections.singleton(new StringValue("1")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            s1 = block(config.getConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            // test delete from set
            ps5.remove(new StringValue("2"));
            try {
                block(configAdmin.updateConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build(),
                    new SetUpdates<>(Collections.emptySet(), Collections.singleton(new StringValue("2")))));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            s1 = block(config.getConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            Assertions.assertThat(ps5).isEqualTo(s1);
            
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet6.getInstance()).add(ADimDef.getInstance().create("VA")).build()));
            block(configAdmin.removeConfigSet(Context.empty(TestSet6.getInstance())));
        }));
    }
    
    @Test
    public void cascadeValueMax() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue3.getInstance()), new LongValue(10L)));
                
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(9L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v2 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(9L), v2);
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build(),
                    new LongValue(10L)));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(11L)));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build(),
                    new LongValue(8L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v3 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(8L), v3);
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(7L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v4 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(7L), v4);
            
            final LongValue v5 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(7L), v5);
            
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue3.getInstance()), new LongValue(6L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v6 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(6L), v6);
            
            final LongValue v7 = block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(6L), v7);
            
            block(configAdmin.removeConfigValue(Context.empty(TestValue3.getInstance())));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build()));
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build()));
            } catch (final IllegalStateException expected) {}
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build()));
            } catch (final IllegalStateException expected) {}
        }));
    }
    
    @Test
    public void dimensionWithMatchAnyValue() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue5.getInstance())
                    .add(DDimDef.getInstance().createMatchAnyDimension())
                    .build(),
                    new LongValue(9L)));
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue5.getInstance()).add(DDimDef.getInstance().create(10L)).build(),
                    new LongValue(2L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            LongValue v5 = block(config.getConfigValue(Context.newBuilder(TestValue5.getInstance())
                .add(DDimDef.getInstance().create(50L))
                .build())).orElse(null);
            assertEquals(new LongValue(9L), v5);
            v5 = block(config.getConfigValue(Context.newBuilder(TestValue5.getInstance()).add(DDimDef.getInstance().create(10L)).build())).orElse(null);
            assertEquals(new LongValue(2L), v5);
        }));
    }
    
    @Test
    public void dimensionWithLargerThanValue() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue6.getInstance()).add(EDimDef.getInstance().create(6L)).build(),
                    new LongValue(1L)));
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue6.getInstance()).add(EDimDef.getInstance().create(12L)).build(),
                    new LongValue(2L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            for (int counter = 1; counter <= 12; counter++) {
                final LongValue v2 = block(config.getConfigValue(Context.newBuilder(TestValue6.getInstance())
                    .add(EDimDef.getInstance().create((long) counter))
                    .build())).orElse(null);
                if (counter < 7) {
                    assertEquals(new LongValue(1L), v2); // 1 < counter < 7 ---> should return 1L
                } else {
                    assertEquals(new LongValue(2L), v2); // 7 < counter < 13 ---> should return 2L
                }
            }
        }));
    }
    
    @Test(expected = UndefinedPropertyException.class)
    public void dimensionWithLargerThanValueFailure() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue6.getInstance()).add(EDimDef.getInstance().create(12L)).build(),
                    new LongValue(2L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // Lookup of TestProp6 with EDimDef value 13L should be undefined, since 12L < 13L
            block(config.getConfigValue(Context.newBuilder(TestValue6.getInstance()).add(EDimDef.getInstance().create(13L)).build()));
        }));
    }
    
    @Test
    public void dimensionWithSmallerThanValue() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue7.getInstance()).add(FDimDef.getInstance().create(6L)).build(),
                    new LongValue(1L)));
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue7.getInstance()).add(FDimDef.getInstance().create(12L)).build(),
                    new LongValue(2L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            for (int counter = 6; counter <= 24; counter++) {
                final LongValue v2 = block(config.getConfigValue(Context.newBuilder(TestValue7.getInstance())
                    .add(FDimDef.getInstance().create((long) counter))
                    .build())).orElse(null);
                if (counter < 12) {
                    assertEquals(new LongValue(1L), v2); // 6 < counter < 12 ---> should return 1L
                } else {
                    assertEquals(new LongValue(2L), v2); // 12 < counter ---> should return 2L
                }
            }
        }));
    }
    
    @Test(expected = UndefinedPropertyException.class)
    public void dimensionWithSmallerThanValueFailure() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue7.getInstance()).add(FDimDef.getInstance().create(2L)).build(),
                    new LongValue(2L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            // Lookup of TestProp7 with FDimDef value 1L should be undefined, since 2L > 1L
            block(config.getConfigValue(Context.newBuilder(TestValue7.getInstance()).add(FDimDef.getInstance().create(1L)).build()));
        }));
    }
    
    @Test
    public void dimensionRequired() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue8.getInstance()), new LongValue(2L)));
                fail();
            } catch (final IllegalArgumentException ignored) {} catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue8.getInstance()).add(ADimDef.getInstance().create("A")).build(),
                    new LongValue(1L)));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v1 = block(config.getConfigValue(Context.newBuilder(TestValue8.getInstance())
                .add(ADimDef.getInstance().create("A"))
                .build())).orElse(null);
            assertEquals(new LongValue(1L), v1);
            
            final List<ConfigValueEntity<LongValue, TestValue8>> values = block(configAdmin.getAllValuesContainingContext(Context
                .empty(TestValue8.getInstance())));
            assertEquals(1, values.size());
        }));
    }
    
    @Test
    public void cascadeValueMin() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue4.getInstance()), new LongValue(10L)));
                
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(11L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v2 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(11L), v2);
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build(),
                    new LongValue(10L)));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(9L)));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build(),
                    new LongValue(15L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v3 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(15L), v3);
            
            try {
                block(configAdmin.setConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    new LongValue(16L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v4 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(16L), v4);
            
            final LongValue v5 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(16L), v5);
            
            try {
                block(configAdmin.setConfigValue(Context.empty(TestValue4.getInstance()), new LongValue(17L)));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final LongValue v6 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build())).orElse(null);
            assertEquals(new LongValue(17L), v6);
            
            final LongValue v7 = block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build())).orElse(null);
            assertEquals(new LongValue(17L), v7);
            
            block(configAdmin.removeConfigValue(Context.empty(TestValue4.getInstance())));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            
            block(configAdmin.removeConfigValue(Context.newBuilder(TestValue4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1", "2")))
                .build()));
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build()));
            } catch (final IllegalStateException expected) {}
            
            try {
                block(config.getConfigValue(Context.newBuilder(TestValue4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1", "2")))
                    .build()));
            } catch (final IllegalStateException expected) {}
        }));
    }
    
    @Test
    public void cascadeSetMax() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Set<StringValue> ps1 = new HashSet<>(3);
            final Set<StringValue> ps2 = new HashSet<>(3);
            
            try {
                ps1.add(new StringValue("1"));
                ps1.add(new StringValue("2"));
                ps1.add(new StringValue("3"));
                block(configAdmin.setConfigSet(Context.empty(TestSet3.getInstance()), ps1));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s1 = block(config.getConfigSet(Context.empty(TestSet3.getInstance())));
            Assertions.assertThat(ps1).isEqualTo(s1);
            
            try {
                ps2.add(new StringValue("1"));
                ps2.add(new StringValue("4"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    ps2));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                ps2.remove(new StringValue("4"));
                ps2.add(new StringValue("2"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet3.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    ps2));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s2 = block(config.getConfigSet(Context.newBuilder(TestSet3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            Assertions.assertThat(ps2).isEqualTo(s2);
            
            try {
                ps1.remove(new StringValue("2"));
                block(configAdmin.setConfigSet(Context.empty(TestSet3.getInstance()), ps1));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s3 = block(config.getConfigSet(Context.empty(TestSet3.getInstance())));
            Assertions.assertThat(ps1).isEqualTo(s3);
            
            ps2.remove(new StringValue("2"));
            final Set<StringValue> s4 = block(config.getConfigSet(Context.newBuilder(TestSet3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            Assertions.assertThat(ps2).isEqualTo(s4);
            
            block(configAdmin.removeConfigSet(Context.empty(TestSet3.getInstance())));
            
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet3.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
        }));
    }
    
    @Test
    public void cascadeSetMin() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Set<StringValue> ps1 = new HashSet<>(3);
            final Set<StringValue> ps2 = new HashSet<>(3);
            
            try {
                ps1.add(new StringValue("1"));
                ps1.add(new StringValue("2"));
                ps1.add(new StringValue("3"));
                block(configAdmin.setConfigSet(Context.empty(TestSet4.getInstance()), ps1));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s1 = block(config.getConfigSet(Context.empty(TestSet4.getInstance())));
            Assertions.assertThat(ps1).isEqualTo(s1);
            
            try {
                ps2.add(new StringValue("1"));
                ps2.add(new StringValue("2"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    ps2));
                fail();
            } catch (final ConfigValidationException expected) {}
            
            try {
                ps2.add(new StringValue("3"));
                ps2.add(new StringValue("4"));
                block(configAdmin.setConfigSet(Context.newBuilder(TestSet4.getInstance())
                    .add(PDimDef.getInstance().create(new Part("1")))
                    .build(),
                    ps2));
                
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s2 = block(config.getConfigSet(Context.newBuilder(TestSet4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            Assertions.assertThat(ps2).isEqualTo(s2);
            
            try {
                ps1.add(new StringValue("5"));
                block(configAdmin.setConfigSet(Context.empty(TestSet4.getInstance()), ps1));
            } catch (final ConfigValidationException e) {
                fail(e.getMessage());
            }
            
            final Set<StringValue> s3 = block(config.getConfigSet(Context.empty(TestSet4.getInstance())));
            Assertions.assertThat(ps1).isEqualTo(s3);
            
            ps2.add(new StringValue("5"));
            final Set<StringValue> s4 = block(config.getConfigSet(Context.newBuilder(TestSet4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
            Assertions.assertThat(ps2).isEqualTo(s4);
            
            block(configAdmin.removeConfigSet(Context.empty(TestSet4.getInstance())));
            
            block(configAdmin.removeConfigSet(Context.newBuilder(TestSet4.getInstance())
                .add(PDimDef.getInstance().create(new Part("1")))
                .build()));
        }));
    }
    
    @Test(expected = IllegalStateException.class)
    public void testRetrieveNullSet() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            block(configAdmin.removeConfigSet(Context.empty(TestSet7.getInstance())));
            
            // Expect an exception from the following line
            block(config.getConfigSet(Context.empty(TestSet7.getInstance())));
        }));
    }
    
}

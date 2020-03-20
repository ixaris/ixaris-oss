package com.ixaris.commons.protobuf.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static valid.Valid.FieldValidation.Type.ALL_OR_NONE;
import static valid.Valid.FieldValidation.Type.AT_LEAST;
import static valid.Valid.FieldValidation.Type.AT_MOST;
import static valid.Valid.FieldValidation.Type.EXACTLY;
import static valid.Valid.FieldValidation.Type.HAS_TEXT;
import static valid.Valid.FieldValidation.Type.IN;
import static valid.Valid.FieldValidation.Type.NOT_IN;
import static valid.Valid.FieldValidation.Type.REQUIRED;
import static valid.Valid.FieldValidation.Type.SIZE;
import static valid.Valid.FieldValidation.Type.TYPE;

import java.util.Arrays;

import org.junit.Test;

import com.ixaris.commons.protobuf.lib.example.Example.AllOrNoneTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.AtLeastNTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.AtMostNTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.BooleanTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.EnumTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.ExactlyNTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.ListTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.MapTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.MessageTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.Nested;
import com.ixaris.commons.protobuf.lib.example.Example.Nested.E;
import com.ixaris.commons.protobuf.lib.example.Example.NumberTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.StringTestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.TestMessage;
import com.ixaris.commons.protobuf.lib.example.Example.TestMessage.SomeOtherMessage;

import valid.Valid.FieldValidation;
import valid.Valid.MessageValidation;

public class MessageValidatorTest {
    
    @Test
    public void testResolveValidator() {
        
        MessageValidation vr = MessageValidator.validate(TestMessage.newBuilder()
            .setStr("aaaaab")
            .setNested(Nested.newBuilder().setA("A").build())
            .setSomeOtherMessage(SomeOtherMessage.newBuilder().setStr("abc").build())
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(TestMessage.newBuilder().setNested(Nested.newBuilder().setB("B")).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("str", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        assertEquals("nested.a", vr.getFields(1).getName());
        assertEquals(REQUIRED, vr.getFields(1).getErrors(0).getType());
        assertEquals("some_other_message", vr.getFields(2).getName());
        assertEquals(REQUIRED, vr.getFields(2).getErrors(0).getType());
        assertEquals("", vr.getFields(3).getName());
        assertEquals(AT_LEAST, vr.getFields(3).getErrors(0).getType());
        assertEquals(Arrays.asList("1", "str", "str2"), vr.getFields(3).getErrors(0).getParamsList());
    }
    
    @Test
    public void testString() {
        
        MessageValidation vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals(1, vr.getFieldsCount());
        assertEquals("s_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSHasText("A").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSHasText("  ").build());
        
        assertTrue(vr.getInvalid());
        assertEquals(1, vr.getFieldsCount());
        assertEquals("s_has_text", vr.getFields(0).getName());
        assertEquals(HAS_TEXT, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSSize("AAAAA").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSSize("AA").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("s_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "8"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSSize("AAAAAAAAA").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("s_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "8"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSIn("ABC").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("s_in", vr.getFields(0).getName());
        assertEquals(IN, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("AAA", "BBB", "CCC"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSIn("AAA").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").putSInEnum("A", "A").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").putSInEnum("AAA", "AAA").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("s_in_enum[AAA", vr.getFields(0).getName());
        assertEquals(TYPE, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder().setSRequired("test").setSNotIn("AAA").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("s_not_in", vr.getFields(0).getName());
        assertEquals(NOT_IN, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("AAA", "BBB", "CCC"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder()
            .setSRequired("test")
            .addAllSl(Arrays.asList("AAAAA", "AAAAAA", "AAAAAAA", "AAAAAAAA"))
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(StringTestMessage.newBuilder()
            .setSRequired("test")
            .addAllSl(Arrays.asList("AAAAA", "A", "     ", "AAAAAAAA", "AAAAAAAAA"))
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("sl[1]", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "8"), vr.getFields(0).getErrors(0).getParamsList());
        assertEquals("sl[2]", vr.getFields(1).getName());
        assertEquals(HAS_TEXT, vr.getFields(1).getErrors(0).getType());
        assertEquals("sl[4]", vr.getFields(2).getName());
        assertEquals(SIZE, vr.getFields(2).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "8"), vr.getFields(2).getErrors(0).getParamsList());
    }
    
    @Test
    public void testBoolean() {
        
        MessageValidation vr = MessageValidator.validate(BooleanTestMessage.newBuilder().setBRequired(true).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(BooleanTestMessage.newBuilder().setBRequired(false).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("b_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(BooleanTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("b_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
    }
    
    @Test
    public void testInt() {
        
        MessageValidation vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(0).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNGe(3).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNGe(2).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_ge", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNGt(4).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNGt(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_gt", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "", "true"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNLe(10).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNLe(11).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_le", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("", "10", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNLt(9).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNLt(10).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_lt", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("", "10", "true"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNRange(10).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNRange(1).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_range", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "10.6", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNRange(11).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_range", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "10.6", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(3).setNGeRef(3).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(4).setNGeRef(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_ge_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("n_required", "", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(2).setNGtRef(3).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(3).setNGtRef(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_gt_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("n_required", "", "true"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(3).setNLeRef(3).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(2).setNLeRef(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_le_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("", "n_required", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(4).setNLtRef(3).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(3).setNLtRef(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_lt_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("", "n_required", "true"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setN(5).setNRequired(10).setNRangeRef(8).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setN(5).setNRequired(10).setNRangeRef(3).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_range_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("n", "n_required", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setN(5).setNRequired(10).setNRangeRef(20).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_range_ref", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("n", "n_required", "false"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNIn(15).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).setNIn(21).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("n_in", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.IN, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "10.0", "15.0", "20"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).addAllNl(Arrays.asList(8, 5, 10)).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(NumberTestMessage.newBuilder().setNRequired(1).addAllNl(Arrays.asList(1, 8, 5, 11, 10)).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("nl[0]", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "10.6", "false"), vr.getFields(0).getErrors(0).getParamsList());
        assertEquals("nl[3]", vr.getFields(1).getName());
        assertEquals(FieldValidation.Type.RANGE, vr.getFields(1).getErrors(0).getType());
        assertEquals(Arrays.asList("5", "10.6", "false"), vr.getFields(1).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMessage() {
        
        MessageValidation vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("m_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setM(Nested.newBuilder().setB("B").build())
            .setMRequired(Nested.newBuilder().setA("A").build())
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("m.a", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .addMl(Nested.newBuilder().setA("A").build())
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .addMl(Nested.newBuilder().build())
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("ml[0].a", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .addMl(Nested.newBuilder().setB("B").build())
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("ml[0].a", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .putMm("A", Nested.newBuilder().setA("A").build())
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .putMm("A", Nested.getDefaultInstance())
            .build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(MessageTestMessage.newBuilder()
            .setMRequired(Nested.newBuilder().setA("A").build())
            .putMm("A", Nested.newBuilder().setB("B").build())
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("mm[A].a", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
    }
    
    @Test
    public void testEnum() {
        // we cannot easily test UNRECOGNIZED
        
        MessageValidation vr = MessageValidator.validate(EnumTestMessage.newBuilder().setERequired(E.A).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(EnumTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("e_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(EnumTestMessage.newBuilder().setERequired(E.A).setEIn(E.A).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(EnumTestMessage.newBuilder().setERequired(E.A).setEIn(E.B).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("e_in", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.IN, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("A", "D"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(EnumTestMessage.newBuilder().setERequired(E.A).setENotIn(E.B).build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(EnumTestMessage.newBuilder().setERequired(E.A).setENotIn(E.A).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("e_not_in", vr.getFields(0).getName());
        assertEquals(FieldValidation.Type.NOT_IN, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("A", "D"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testList() {
        MessageValidation vr = MessageValidator.validate(ListTestMessage.newBuilder().addLRequired("A").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(ListTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("l_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        
        vr = MessageValidator.validate(ListTestMessage.newBuilder().addLRequired("A").addLSize("A").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("l_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(ListTestMessage.newBuilder().addLRequired("A").addAllLSize(Arrays.asList("A", "A", "A", "A", "A")).build());
        
        assertTrue(vr.getInvalid());
        assertEquals("l_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMap() {
        MessageValidation vr = MessageValidator.validate(MapTestMessage.newBuilder().putImRequired(1, "A").putSmRequired("A", "A").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(MapTestMessage.newBuilder().build());
        
        assertTrue(vr.getInvalid());
        assertEquals("sm_required", vr.getFields(0).getName());
        assertEquals(REQUIRED, vr.getFields(0).getErrors(0).getType());
        assertEquals("im_required", vr.getFields(1).getName());
        assertEquals(REQUIRED, vr.getFields(1).getErrors(0).getType());
        
        vr = MessageValidator.validate(MapTestMessage.newBuilder().putImRequired(1, "A").putSmRequired("A", "A").putImSize(1, "A").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("im_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(MapTestMessage.newBuilder().putImRequired(1, "A").putSmRequired("A", "A").putSmSize("A", "A").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("sm_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(MapTestMessage.newBuilder()
            .putImRequired(1, "A")
            .putSmRequired("A", "A")
            .putImSize(1, "A")
            .putImSize(2, "A")
            .putImSize(3, "A")
            .putImSize(4, "A")
            .putImSize(5, "A")
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("im_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(MapTestMessage.newBuilder()
            .putImRequired(1, "A")
            .putSmRequired("A", "A")
            .putSmSize("A1", "A")
            .putSmSize("A2", "A")
            .putSmSize("A3", "A")
            .putSmSize("A4", "A")
            .putSmSize("A5", "A")
            .build());
        
        assertTrue(vr.getInvalid());
        assertEquals("sm_size", vr.getFields(0).getName());
        assertEquals(SIZE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("2", "4"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMessageAtLeastN() {
        
        MessageValidation vr = MessageValidator.validate(AtLeastNTestMessage.newBuilder().setA("A").setC("C").setD("D").setE("E").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AtLeastNTestMessage.newBuilder().setA("A").setB("B").setC("C").setD("D").setE("E").setF("F").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AtLeastNTestMessage.newBuilder().setC("C").setD("D").setF("F").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(AT_LEAST, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("1", "a", "b"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(AtLeastNTestMessage.newBuilder().setB("B").setC("C").setE("E").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(AT_LEAST, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "c", "d", "e", "f"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMessageAtMostN() {
        
        MessageValidation vr = MessageValidator.validate(AtMostNTestMessage.newBuilder().build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AtMostNTestMessage.newBuilder().setA("A").setF("F").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AtMostNTestMessage.newBuilder().setB("B").setC("C").setD("D").setE("E").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AtMostNTestMessage.newBuilder().setA("A").setB("B").setD("D").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(AT_MOST, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("1", "a", "b"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(AtMostNTestMessage.newBuilder().setC("C").setD("D").setE("E").setF("F").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(AT_MOST, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "c", "d", "e", "f"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMessageExactlyN() {
        
        MessageValidation vr = MessageValidator.validate(ExactlyNTestMessage.newBuilder().setA("A").setC("C").setD("D").setE("E").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(ExactlyNTestMessage.newBuilder().setB("B").setC("C").setE("E").setF("F").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(ExactlyNTestMessage.newBuilder().setA("A").setB("B").setC("C").setD("E").setE("E").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(EXACTLY, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("1", "a", "b"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(ExactlyNTestMessage.newBuilder().setA("A").setC("C").setD("E").setE("E").setF("F").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(EXACTLY, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "c", "d", "e", "f"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(ExactlyNTestMessage.newBuilder().setA("A").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(EXACTLY, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("3", "c", "d", "e", "f"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
    @Test
    public void testMessageAllOrNone() {
        
        MessageValidation vr = MessageValidator.validate(AllOrNoneTestMessage.newBuilder().setA("A").setB("B").setC("C").build());
        
        assertFalse(vr.getInvalid());
        assertEquals(0, vr.getFieldsCount());
        
        vr = MessageValidator.validate(AllOrNoneTestMessage.newBuilder().setA("A").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(ALL_OR_NONE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("a", "b", "c"), vr.getFields(0).getErrors(0).getParamsList());
        
        vr = MessageValidator.validate(AllOrNoneTestMessage.newBuilder().setB("B").setC("C").build());
        
        assertTrue(vr.getInvalid());
        assertEquals("", vr.getFields(0).getName());
        assertEquals(ALL_OR_NONE, vr.getFields(0).getErrors(0).getType());
        assertEquals(Arrays.asList("a", "b", "c"), vr.getFields(0).getErrors(0).getParamsList());
    }
    
}

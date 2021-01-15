package com.ixaris.commons.iso8583.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.ixaris.commons.iso8583.lib.Base24Msg.Field;

public class ISOTest {
    
    @Test
    public void test() throws IOException {
        
        final Base24Msg m = Base24Utils.base24QueryCardBalanceMsg(Base24IssuerParams.eur(),
            "4444333322221111",
            Base24Utils.CURRENCY_EUR,
            "0101",
            "101010",
            "123456789012");
        
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        Base24Packager.getInstance().assemble(m, os);
        byte[] data = os.toByteArray();
        System.out.println(new String(data));
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data);
        Base24Msg m2 = Base24Packager.getInstance().disassemble(is);
        
        System.out.println(m2.getValue(Field.TRACK_2_DATA));
    }
    
    @Test
    public void test2() throws IOException {
        
        final DataInputStream dis = new DataInputStream(getClass().getResourceAsStream("/F0015304_15-04-25"));
        
        while (true) {
            try {
                dis.readShort();
            } catch (EOFException e) {
                break;
            }
            
            final int len = dis.readShort();
            final byte[] tmp = new byte[len];
            dis.readFully(tmp);
            
            byte[] orig = new byte[len + 2];
            orig[1] = (byte) len;
            orig[0] = (byte) (len >>> 8);
            System.arraycopy(tmp, 0, orig, 2, tmp.length);
            
            final MasterCardIpmMsg m = MasterCardIpmPackager.getInstance().disassemble(new ByteArrayInputStream(orig));
            
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            MasterCardIpmPackager.getInstance().assemble(m, os);
            byte[] data = os.toByteArray();
            
            Assert.assertTrue(Arrays.equals(orig, data));
            System.out.println(new String(data));
            
            final ByteArrayInputStream is = new ByteArrayInputStream(data);
            MasterCardIpmMsg m2 = MasterCardIpmPackager.getInstance().disassemble(is);
        }
    }
    
    @Test
    public void test3() throws IOException {
        
        final byte[] orig = ISOPackager
            .fromHex(
                "39323230f23e46952fe0852000000002040000223136353339393833303133303633303638323031313030303030303030303230303030303038323832323135333930303630353732333135333930383238313830323038323830303030393031303030303043303030303030303043303030303030303030363530363134333234353339393833303133303633303638323d31383032323231303030323635393233313430313037313833303032323131414d4230393031414d42303930313030303030303031415348414b4520484f55534520414745474520202020204c41474f5320202020202020204c474e47353636303230313035333536364430303030303032303030303030303431353130303130303036363638313537353030303030303230303030303030303030303030303030304330303030303030304330303030303030303138323438303239373034393031303035393030303135353131323031323133333434303032303030343238641c1420000000003130303036363638313537354f4c444d4642737263202020414343494f4e535754736e6b3030363035372020202020204754424d434465626974202031313132414343494f4e4d4642736e6b3031323334303030303030202020353636414343494f4e2020323031353038333130303330353231324d6564696142617463684e7231363333373537343231314d65646961546f74616c73333132313c4d65646961546f74616c733e3c546f74616c3e3c416d6f756e743e3232333030303030303c2f416d6f756e743e3c43757272656e63793e3536363c2f43757272656e63793e3c4d65646961436c6173733e436173683c2f4d65646961436c6173733e3c2f546f74616c3e3c2f4d65646961546f74616c733e3231344164646974696f6e616c496e666f333132323c4164646974696f6e616c496e666f3e3c446f776e6c6f61643e3c41544d436f6e66696749443e353030363c2f41544d436f6e66696749443e3c41746d417070436f6e66696749443e353030363c2f41746d417070436f6e66696749443e3c2f446f776e6c6f61643e3c2f4164646974696f6e616c496e666f3e58");
        
        final InterswitchMsg m = InterswitchPackager.getInstance().disassemble(new ByteArrayInputStream(orig));
        final UnmodifiableByteArray sv = m.getByteValue(127);
        final InterswitchSubMsg127 sm = InterswitchSubMsg127Packager.getInstance().disassemble(new DataInputStream(sv.getInputStream()));
        
        final ByteArrayOutputStream sos = new ByteArrayOutputStream();
        InterswitchSubMsg127Packager.getInstance().assemble(sm, sos);
        m.set(InterswitchMsg.Field.POSTILION_PRIVATE_DATA, sos.toByteArray());
        Assert.assertTrue(sv.equals(m.getByteValue(127)));
        
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        InterswitchPackager.getInstance().assemble(m, os);
        byte[] data = os.toByteArray();
        
        Assert.assertTrue(Arrays.equals(orig, data));
        System.out.println(new String(data));
        
        final ByteArrayInputStream is = new ByteArrayInputStream(data);
        final InterswitchMsg m2 = InterswitchPackager.getInstance().disassemble(is);
        final InterswitchSubMsg127 sm2 = InterswitchSubMsg127Packager
            .getInstance()
            .disassemble(new DataInputStream(m.getByteValue(127).getInputStream()));
    }
    
}

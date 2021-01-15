package com.ixaris.commons.iso8583.lib;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.SortedMap;

import com.ixaris.commons.iso8583.lib.ISOMsg.ISOField;

public abstract class ISOPackager {
    
    public enum MtiType {
        NONE,
        BIN,
        ASCII;
    }
    
    public enum VarFieldLengthType {
        BIN,
        ASCII;
    }
    
    public static boolean isEven(final int num) {
        return (num & 1) == 0x0;
    }
    
    public static byte[] fromHex(final String hex) {
        if (isEven(hex.length())) {
            final byte[] ret = new byte[hex.length() / 2];
            
            for (int i = 0; i < ret.length; i++) {
                ret[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            
            return ret;
        } else {
            final byte[] ret = new byte[(hex.length() / 2) + 1];
            
            for (int i = 0; i < ret.length - 1; i++) {
                ret[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            
            ret[ret.length - 1] = (byte) Integer.parseInt(hex.substring((ret.length - 1) * 2, (ret.length - 1) * 2 + 1), 16);
            
            return ret;
        }
    }
    
    public static String toHex(final int length, final byte[] data) {
        final StringBuilder ret = new StringBuilder();
        
        if (isEven(length)) {
            for (byte datum : data) {
                String hex = Integer.toHexString(0xFF & (int) datum);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                ret.append(hex);
            }
        } else {
            for (int i = 0; i < data.length - 1; i++) {
                String hex = Integer.toHexString(0xFF & (int) data[i]);
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                ret.append(hex);
            }
            
            ret.append(Integer.toHexString(0xFF & (int) data[data.length - 1]));
        }
        
        return ret.toString().toUpperCase();
    }
    
    public static String toHex(final int length, final UnmodifiableByteArray data) {
        final StringBuilder ret = new StringBuilder();
        
        if (isEven(length)) {
            for (int i = 0; i < data.getLength(); i++) {
                String hex = Integer.toHexString(0xFF & (int) data.get(i));
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                ret.append(hex);
            }
        } else {
            for (int i = 0; i < data.getLength() - 1; i++) {
                String hex = Integer.toHexString(0xFF & (int) data.get(i));
                if (hex.length() == 1) {
                    hex = "0" + hex;
                }
                ret.append(hex);
            }
            
            ret.append(Integer.toHexString(0xFF & (int) data.get(data.getLength() - 1)));
        }
        
        return ret.toString().toUpperCase();
    }
    
    private static boolean isBitmapFieldSet(final byte[] bitMap, final int index) {
        int byteInBitMap = (index - 1) >>> 3;
        int bitInBitMap = (index - 1) & 0x7;
        
        return (bitMap[byteInBitMap] & (1 << (7 - bitInBitMap))) != 0;
    }
    
    private static byte binaryCodedDecimal(final int dec) {
        if ((dec < 0) || (dec > 99)) {
            throw new IllegalArgumentException("Invalid decimal [" + dec + "]");
        }
        
        return (byte) (((dec / 10) << 4) + (dec % 10));
    }
    
    private static int determineFieldHeaderLength(final ISOFieldSpec fieldSpec, final boolean binLength) {
        
        switch (fieldSpec.getFormat()) {
            case FIXED:
                return 0;
            case HLVAR:
                return 1;
            case HLLVAR:
                return binLength ? 1 : 2;
            case HLLLVAR:
                return binLength ? 2 : 3;
            case HLLLLVAR:
                return binLength ? 2 : 4;
            case HLLLLLVAR:
                return binLength ? 3 : 5;
            case HLLLLLLVAR:
                return binLength ? 3 : 6;
            default:
                throw new UnsupportedOperationException("Unsupported format [" + fieldSpec.getFormat() + "]");
        }
    }
    
    private static int unpackFieldLength(final byte[] length, final ISOFieldSpec fieldSpec, final boolean binLength) {
        
        switch (fieldSpec.getFormat()) {
            case FIXED:
                return fieldSpec.getMaxLength();
            case HLVAR:
                if (binLength) {
                    return length[0] & 0xF;
                } else {
                    return length[0] - '0';
                }
            case HLLVAR:
                if (binLength) {
                    return ((length[0] >>> 4) * 10) + (length[0] & 0xF);
                } else {
                    return ((length[0] - '0') * 10) + (length[1] - '0');
                }
            case HLLLVAR:
                if (binLength) {
                    return ((length[0] & 0xF) * 100) + ((length[1] >>> 4) * 10) + (length[1] & 0xF);
                } else {
                    return ((length[0] - '0') * 100) + ((length[1] - '0') * 10) + (length[2] - '0');
                }
            case HLLLLVAR:
                if (binLength) {
                    return ((length[0] >>> 4) * 1000) + ((length[0] & 0xF) * 100) + ((length[1] >>> 4) * 10) + (length[1] & 0xF);
                } else {
                    return ((length[0] - '0') * 1000) + ((length[1] - '0') * 100) + ((length[2] - '0') * 10) + (length[3] - '0');
                }
            case HLLLLLVAR:
                if (binLength) {
                    return ((length[0] & 0xF) * 10000)
                        + ((length[1] >>> 4) * 1000)
                        + ((length[1] & 0xF) * 100)
                        + ((length[2] >>> 4) * 10)
                        + (length[2] & 0xF);
                } else {
                    return ((length[0] - '0') * 10000)
                        + ((length[1] - '0') * 1000)
                        + ((length[2] - '0') * 100)
                        + ((length[3] - '0') * 10)
                        + (length[4] - '0');
                }
            case HLLLLLLVAR:
                if (binLength) {
                    return ((length[0] >>> 4) * 100000)
                        + ((length[0] & 0xF) * 10000)
                        + ((length[1] >>> 4) * 1000)
                        + ((length[1] & 0xF) * 100)
                        + ((length[2] >>> 4) * 10)
                        + (length[2] & 0xF);
                } else {
                    return ((length[0] - '0') * 100000)
                        + ((length[1] - '0') * 10000)
                        + ((length[2] - '0') * 1000)
                        + ((length[3] - '0') * 100)
                        + ((length[4] - '0') * 10)
                        + (length[5] - '0');
                }
            default:
                throw new UnsupportedOperationException("Unsupported format [" + fieldSpec.getFormat() + "]");
        }
    }
    
    public static byte[] packFieldLength(final int length, final ISOFieldSpec fieldSpec, final boolean binLength) {
        
        switch (fieldSpec.getFormat()) {
            case FIXED:
                return null;
            
            case HLVAR:
                final byte[] lPacked = new byte[1];
                if (binLength) {
                    lPacked[0] = binaryCodedDecimal(length);
                } else {
                    lPacked[0] = (byte) ('0' + length);
                }
                return lPacked;
            
            case HLLVAR:
                final byte[] llPacked;
                if (binLength) {
                    llPacked = new byte[1];
                    llPacked[0] = binaryCodedDecimal(length);
                } else {
                    llPacked = new byte[2];
                    llPacked[0] = (byte) ('0' + (length / 10));
                    llPacked[1] = (byte) ('0' + (length % 10));
                }
                return llPacked;
            
            case HLLLVAR:
                final byte[] lllPacked;
                if (binLength) {
                    lllPacked = new byte[2];
                    lllPacked[0] = binaryCodedDecimal(length / 100);
                    lllPacked[1] = binaryCodedDecimal(length % 100);
                } else {
                    lllPacked = new byte[3];
                    lllPacked[0] = (byte) ('0' + (length / 100));
                    lllPacked[1] = (byte) ('0' + ((length % 100) / 10));
                    lllPacked[2] = (byte) ('0' + (length % 10));
                }
                return lllPacked;
            
            case HLLLLVAR:
                final byte[] llllPacked;
                if (binLength) {
                    llllPacked = new byte[2];
                    llllPacked[0] = binaryCodedDecimal(length / 100);
                    llllPacked[1] = binaryCodedDecimal(length % 100);
                } else {
                    llllPacked = new byte[4];
                    llllPacked[0] = (byte) ('0' + (length / 1000));
                    llllPacked[1] = (byte) ('0' + ((length % 1000) / 100));
                    llllPacked[2] = (byte) ('0' + ((length % 100) / 10));
                    llllPacked[3] = (byte) ('0' + (length % 10));
                }
                return llllPacked;
            
            case HLLLLLVAR:
                final byte[] lllllPacked;
                if (binLength) {
                    lllllPacked = new byte[3];
                    lllllPacked[1] = binaryCodedDecimal(length / 10000);
                    lllllPacked[1] = binaryCodedDecimal((length % 10000) / 100);
                    lllllPacked[2] = binaryCodedDecimal(length % 100);
                } else {
                    lllllPacked = new byte[5];
                    lllllPacked[0] = (byte) ('0' + (length / 10000));
                    lllllPacked[1] = (byte) ('0' + ((length % 10000) / 1000));
                    lllllPacked[2] = (byte) ('0' + ((length % 1000) / 100));
                    lllllPacked[3] = (byte) ('0' + ((length % 100) / 10));
                    lllllPacked[4] = (byte) ('0' + (length % 10));
                }
                return lllllPacked;
            
            case HLLLLLLVAR:
                final byte[] llllllPacked;
                if (binLength) {
                    llllllPacked = new byte[3];
                    llllllPacked[1] = binaryCodedDecimal(length / 10000);
                    llllllPacked[1] = binaryCodedDecimal((length % 10000) / 100);
                    llllllPacked[2] = binaryCodedDecimal(length % 100);
                } else {
                    llllllPacked = new byte[6];
                    llllllPacked[0] = (byte) ('0' + (length / 100000));
                    llllllPacked[1] = (byte) ('0' + ((length % 100000) / 10000));
                    llllllPacked[2] = (byte) ('0' + ((length % 10000) / 1000));
                    llllllPacked[3] = (byte) ('0' + ((length % 1000) / 100));
                    llllllPacked[4] = (byte) ('0' + ((length % 100) / 10));
                    llllllPacked[5] = (byte) ('0' + (length % 10));
                }
                return llllllPacked;
            
            default:
                throw new UnsupportedOperationException("Unsupported format [" + fieldSpec.getFormat() + "]");
        }
    }
    
    private static int determineFieldLengthInBytes(final int length, final ISOFieldSpec fieldSpec) {
        
        switch (fieldSpec.getCoding()) {
            case ASCII:
            case BIN:
                return length;
            // case BCD:
            // return (length / 2) + (isEven(length) ? 0 : 1); // cater for odd bcd where first bcd takes up a whole
            // byte
            default:
                throw new UnsupportedOperationException("Unsupported coding [" + fieldSpec.getCoding() + "]");
        }
    }
    
    private final MtiType mtiType;
    private final boolean binLength;
    private final Charset charset;
    private final ISOFieldSpec[] fieldSpecs;
    
    public ISOPackager(final MtiType mtiType,
                       final VarFieldLengthType varFieldLengthType,
                       final Charset charset,
                       final ISOFieldSpec... fieldSpecs) {
        
        this.mtiType = mtiType;
        this.binLength = varFieldLengthType.equals(VarFieldLengthType.BIN);
        this.charset = charset;
        this.fieldSpecs = fieldSpecs;
        
        // basic validation, make sure there are 64, 128 or 192 fields + bitmap field ar position 0
        if ((fieldSpecs.length != 65) && (fieldSpecs.length != 129) && (fieldSpecs.length != 193)) {
            throw new IllegalArgumentException("Should have PRIMARY BITMAP + 64 / 128 / 192 fields specified");
        }
    }
    
    public final void unpack(final ISOMsg m, final InputStream is) throws IOException {
        
        final DataInputStream dis = new DataInputStream(is);
        
        switch (mtiType) {
            case BIN:
                final byte[] bmti = new byte[2];
                dis.readFully(bmti, 0, bmti.length);
                m.setMTI(bmti);
                // , "" + (data[index] >>> 4) + (data[index] & 0xF) + (data[index + 1] >>> 4) + (data[index + 1] & 0xF));
                break;
            case ASCII:
                final byte[] amti = new byte[4];
                dis.readFully(amti, 0, amti.length);
                m.setMTI(amti);
                // , "" + (char)data[index] + (char)data[index+1] + (char)data[index+2] + (char)data[index+3]);
                break;
            default:
                // skip
        }
        
        final byte[] primaryBitMap;
        if (FieldContentCoding.BIN.equals(fieldSpecs[0].getCoding())) {
            primaryBitMap = new byte[8];
            dis.readFully(primaryBitMap, 0, primaryBitMap.length);
        } else if (FieldContentCoding.ASCII.equals(fieldSpecs[0].getCoding())) {
            final byte[] pbm = new byte[16];
            dis.readFully(pbm, 0, pbm.length);
            primaryBitMap = fromHex(new String(pbm));
        } else {
            throw new UnsupportedOperationException("Unsupported encoding [" + fieldSpecs[1].getFormat() + "] for bitmap field");
        }
        
        final byte[] secondaryBitMap;
        if (isBitmapFieldSet(primaryBitMap, 1)) {
            if (FieldContentCoding.BIN.equals(fieldSpecs[1].getCoding())) {
                secondaryBitMap = new byte[8];
                dis.readFully(secondaryBitMap, 0, secondaryBitMap.length);
            } else if (FieldContentCoding.ASCII.equals(fieldSpecs[1].getCoding())) {
                final byte[] sbm = new byte[16];
                dis.readFully(sbm, 0, sbm.length);
                secondaryBitMap = fromHex(new String(sbm));
            } else {
                throw new UnsupportedOperationException("Unsupported encoding [" + fieldSpecs[1].getFormat() + "] for bitmap field");
            }
        } else {
            secondaryBitMap = null;
        }
        
        for (int i = 2; i <= 64; i++) {
            if (isBitmapFieldSet(primaryBitMap, i)) {
                final byte[] l = new byte[determineFieldHeaderLength(fieldSpecs[i], binLength)];
                dis.readFully(l, 0, l.length);
                final int length = unpackFieldLength(l, fieldSpecs[i], binLength);
                final byte[] field = new byte[determineFieldLengthInBytes(length, fieldSpecs[i])];
                dis.readFully(field, 0, field.length);
                m.set(i, new ISOField(length, field));
            }
        }
        
        if (secondaryBitMap != null) {
            final byte[] tertiaryBitMap;
            if (isBitmapFieldSet(secondaryBitMap, 1)) {
                if (FieldContentCoding.BIN.equals(fieldSpecs[65].getCoding())) {
                    tertiaryBitMap = new byte[8];
                    dis.readFully(tertiaryBitMap, 0, tertiaryBitMap.length);
                } else if (FieldContentCoding.ASCII.equals(fieldSpecs[65].getCoding())) {
                    final byte[] tbm = new byte[16];
                    dis.readFully(tbm, 0, tbm.length);
                    tertiaryBitMap = fromHex(new String(tbm));
                } else {
                    throw new UnsupportedOperationException("Unsupported encoding [" + fieldSpecs[65].getFormat() + "] for bitmap field");
                }
            } else {
                tertiaryBitMap = null;
            }
            
            for (int i = 66; i <= 128; i++) {
                if (isBitmapFieldSet(secondaryBitMap, i - 64)) {
                    final byte[] l = new byte[determineFieldHeaderLength(fieldSpecs[i], binLength)];
                    dis.readFully(l, 0, l.length);
                    final int length = unpackFieldLength(l, fieldSpecs[i], binLength);
                    final byte[] field = new byte[determineFieldLengthInBytes(length, fieldSpecs[i])];
                    dis.readFully(field, 0, field.length);
                    m.set(i, new ISOField(length, field));
                }
            }
            
            if (tertiaryBitMap != null) {
                for (int i = 129; i <= 192; i++) {
                    if (isBitmapFieldSet(tertiaryBitMap, i - 128)) {
                        final byte[] l = new byte[determineFieldHeaderLength(fieldSpecs[i], binLength)];
                        dis.readFully(l, 0, l.length);
                        final int length = unpackFieldLength(l, fieldSpecs[i], binLength);
                        final byte[] field = new byte[determineFieldLengthInBytes(length, fieldSpecs[i])];
                        dis.readFully(field, 0, field.length);
                        m.set(i, new ISOField(length, field));
                    }
                }
            }
        }
    }
    
    public final void pack(final ISOMsg m, final OutputStream os) throws IOException {
        
        final SortedMap<Integer, ISOField> fields = m.getFields();
        
        // add the MTI (Message Type Indicator)
        final byte[] mti = m.getMTI();
        if (mti != null) {
            os.write(mti);
        }
        
        // add the fields
        for (final Map.Entry<Integer, ISOField> field : fields.entrySet()) {
            final ISOFieldSpec fieldSpec = fieldSpecs[field.getKey()];
            final int length = field.getValue().length;
            
            final byte[] len = packFieldLength(length, fieldSpec, binLength);
            if (len != null) {
                os.write(len);
            }
            field.getValue().write(os);
        }
    }
    
    public String decodeField(final ISOField field, final int index) {
        
        if (field == null) {
            return null;
        }
        
        final ISOFieldSpec fieldSpec = fieldSpecs[index];
        
        switch (fieldSpec.getCoding()) {
            case ASCII:
                return field.toString(charset);
            case BIN:
                // case BCD:
                return toHex(field.length, field.data);
            default:
                throw new UnsupportedOperationException("Coding [" + fieldSpec.getCoding() + "] not found");
        }
    }
    
    public ISOField encodeField(final String str, final int index) {
        
        final ISOFieldSpec fieldSpec = fieldSpecs[index];
        
        switch (fieldSpec.getCoding()) {
            case ASCII:
                final byte[] s;
                if (charset == null) {
                    s = str.getBytes();
                } else {
                    s = str.getBytes(charset);
                }
                return new ISOField(s.length, s);
            case BIN:
                final byte[] b = fromHex(str);
                return new ISOField(b.length, b);
            // case BCD:
            // byte[] x = fromHex(str);
            // return new ISOField(x.length / 2, x);
            default:
                throw new UnsupportedOperationException("Coding [" + fieldSpec.getCoding() + "] not found");
        }
    }
    
    public int determineFieldHeaderLength(final int index) {
        return determineFieldHeaderLength(fieldSpecs[index], binLength);
    }
    
    public ISOField packBitmap(final byte[] data, final int index) {
        
        final ISOFieldSpec fieldSpec = fieldSpecs[index];
        
        switch (fieldSpec.getCoding()) {
            case ASCII:
                return new ISOField(16, toHex(data.length, data).getBytes());
            case BIN:
                return new ISOField(8, data);
            default:
                throw new UnsupportedOperationException("Coding [" + fieldSpec.getCoding() + "] not supported");
        }
    }
    
    public void validateLength(final ISOField field, final int index) {
        
        final ISOFieldSpec fieldSpec = fieldSpecs[index];
        
        if ((field.length < fieldSpec.getMinLength()) || (field.length > fieldSpec.getMaxLength())) {
            throw new IllegalStateException(String.format("Value [%s] is invalid. Length should be between %d and %d",
                decodeField(field, index),
                fieldSpec.getMinLength(),
                fieldSpec.getMaxLength()));
        }
        
        switch (fieldSpec.getCoding()) {
            case ASCII:
            case BIN:
                if (field.length != field.data.getLength()) {
                    throw new IllegalStateException(String.format("Given length [%d] does not match data [%s]",
                        field.length,
                        decodeField(field, index)));
                }
                break;
            // case BCD:
            // if (((field.length + 1) / 2) != field.data.getLength()) {
            // throw new IllegalStateException(String.format("Given length [%d] does not match data [%s]", field.length, decodeField(field, index)));
            // }
            // break;
            default:
                throw new UnsupportedOperationException("Coding [" + fieldSpec.getCoding() + "] not found");
        }
    }
    
}

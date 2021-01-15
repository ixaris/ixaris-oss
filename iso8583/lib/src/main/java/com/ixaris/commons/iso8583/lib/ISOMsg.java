package com.ixaris.commons.iso8583.lib;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class ISOMsg {
    
    public static final int PRIMARY_BIT_MAP_INDEX = 0;
    public static final int SECONDARY_BIT_MAP_INDEX = 1;
    public static final int TERTIARY_BIT_MAP_INDEX = 65;
    public static final int FIELD_MAX_INDEX = 192;
    
    private byte[] mti;
    private final byte[] primaryBitMap = new byte[8];
    private final byte[] secondaryBitMap = new byte[8];
    private final byte[] tertiaryBitMap = new byte[8];
    
    // Stores a map of <DE index, value> pairs for Data Elements (DE) contained in the ISOMsg
    private int length = 0;
    private final SortedMap<Integer, ISOField> fields = new TreeMap<>();
    
    public ISOMsg() {}
    
    protected ISOMsg(final ISOMsg other) {
        
        this.mti = other.mti;
        
        System.arraycopy(other.primaryBitMap, 0, primaryBitMap, 0, other.primaryBitMap.length);
        if (other.secondaryBitMap != null) {
            System.arraycopy(other.secondaryBitMap, 0, secondaryBitMap, 0, other.secondaryBitMap.length);
        }
        if (other.tertiaryBitMap != null) {
            System.arraycopy(other.tertiaryBitMap, 0, tertiaryBitMap, 0, other.tertiaryBitMap.length);
        }
        
        length = other.length;
        fields.putAll(other.fields);
    }
    
    protected abstract ISOPackager getPackager();
    
    protected final void setMTI(final byte[] mti) {
        if (this.mti != null) {
            length -= mti.length;
        }
        this.mti = mti;
        if (this.mti != null) {
            length += mti.length;
        }
    }
    
    public final byte[] getMTI() {
        return mti;
    }
    
    public final int getLength() {
        return length;
    }
    
    public final String getStringValue(final int index) {
        return getPackager().decodeField(fields.get(index), index);
    }
    
    public final UnmodifiableByteArray getByteValue(final int index) {
        return fields.get(index).data;
    }
    
    protected final void set(final int index, final String value) {
        set(index, getPackager().encodeField(value, index));
    }
    
    protected final void set(final int index, final ISOField field) {
        if ((index < SECONDARY_BIT_MAP_INDEX) || (index == TERTIARY_BIT_MAP_INDEX) || (index > FIELD_MAX_INDEX)) {
            throw new IllegalArgumentException("invalid index [" + index + "]");
        }
        
        getPackager().validateLength(field, index);
        
        length += field.length;
        ISOField old = fields.put(index, field);
        if (old == null) {
            length += getPackager().determineFieldHeaderLength(index);
            
            if (index <= 64) {
                final int byteInBitMap = (index - 1) >>> 3;
                final int bitInBitMap = (index - 1) & 0x7;
                
                primaryBitMap[byteInBitMap] |= 1 << (7 - bitInBitMap);
                final ISOField bf = getPackager().packBitmap(primaryBitMap, PRIMARY_BIT_MAP_INDEX);
                if (fields.put(PRIMARY_BIT_MAP_INDEX, bf) == null) {
                    length += bf.length;
                }
                
            } else if (index <= 128) {
                primaryBitMap[0] |= 1 << 7;
                
                int byteInBitMap = (index - 65) >>> 3;
                int bitInBitMap = (index - 65) & 0x7;
                
                secondaryBitMap[byteInBitMap] |= 1 << (7 - bitInBitMap);
                final ISOField bf = getPackager().packBitmap(secondaryBitMap, SECONDARY_BIT_MAP_INDEX);
                if (fields.put(SECONDARY_BIT_MAP_INDEX, bf) == null) {
                    length += bf.length;
                }
                
            } else {
                secondaryBitMap[0] |= 1 << 7;
                
                int byteInBitMap = (index - 129) >>> 3;
                int bitInBitMap = (index - 129) & 0x7;
                
                tertiaryBitMap[byteInBitMap] |= 1 << (7 - bitInBitMap);
                final ISOField bf = getPackager().packBitmap(tertiaryBitMap, TERTIARY_BIT_MAP_INDEX);
                if (fields.put(TERTIARY_BIT_MAP_INDEX, bf) == null) {
                    length += bf.length;
                }
            }
            
        } else {
            length -= old.length;
        }
    }
    
    protected final void unset(final int index) {
        if ((index < SECONDARY_BIT_MAP_INDEX) || (index == TERTIARY_BIT_MAP_INDEX) || (index > FIELD_MAX_INDEX)) {
            throw new IllegalArgumentException("invalid index [" + index + "]");
        }
        
        ISOField old = fields.remove(index);
        
        if (old != null) {
            length -= getPackager().determineFieldHeaderLength(index);
            length -= old.length;
            
            if (index <= 64) {
                final int byteInBitMap = (index - 1) >>> 3;
                final int bitInBitMap = (index - 1) & 0x7;
                
                primaryBitMap[byteInBitMap] &= ~(1 << (7 - bitInBitMap));
                final ISOField bf = getPackager().packBitmap(primaryBitMap, PRIMARY_BIT_MAP_INDEX);
                if (fields.put(PRIMARY_BIT_MAP_INDEX, bf) == null) {
                    length += bf.length;
                }
                
            } else if (index <= 128) {
                int byteInBitMap = (index - 65) >>> 3;
                int bitInBitMap = (index - 65) & 0x7;
                
                secondaryBitMap[byteInBitMap] &= ~(1 << (7 - bitInBitMap));
                
                // check whether we still require secondary bitmap
                if ((secondaryBitMap[0] == 0)
                    && (secondaryBitMap[1] == 0)
                    && (secondaryBitMap[2] == 0)
                    && (secondaryBitMap[3] == 0)
                    && (secondaryBitMap[4] == 0)
                    && (secondaryBitMap[5] == 0)
                    && (secondaryBitMap[6] == 0)
                    && (secondaryBitMap[7] == 0)) {
                    
                    old = fields.remove(SECONDARY_BIT_MAP_INDEX);
                    if (old != null) {
                        length -= old.length;
                    }
                    
                    primaryBitMap[0] &= ~(1 << 7);
                    final ISOField bf = getPackager().packBitmap(primaryBitMap, PRIMARY_BIT_MAP_INDEX);
                    if (fields.put(PRIMARY_BIT_MAP_INDEX, bf) == null) {
                        length += bf.length;
                    }
                } else {
                    final ISOField bf = getPackager().packBitmap(secondaryBitMap, SECONDARY_BIT_MAP_INDEX);
                    if (fields.put(SECONDARY_BIT_MAP_INDEX, bf) == null) {
                        length += bf.length;
                    }
                }
            } else {
                int byteInBitMap = (index - 129) >>> 3;
                int bitInBitMap = (index - 129) & 0x7;
                
                tertiaryBitMap[byteInBitMap] &= ~(1 << (7 - bitInBitMap));
                
                // check whether we still require tertiary bitmap
                if ((tertiaryBitMap[0] == 0)
                    && (tertiaryBitMap[1] == 0)
                    && (tertiaryBitMap[2] == 0)
                    && (tertiaryBitMap[3] == 0)
                    && (tertiaryBitMap[4] == 0)
                    && (tertiaryBitMap[5] == 0)
                    && (tertiaryBitMap[6] == 0)
                    && (tertiaryBitMap[7] == 0)) {
                    
                    old = fields.remove(TERTIARY_BIT_MAP_INDEX);
                    if (old != null) {
                        length -= old.length;
                    }
                    
                    secondaryBitMap[0] &= ~(1 << 7);
                    final ISOField bf = getPackager().packBitmap(secondaryBitMap, SECONDARY_BIT_MAP_INDEX);
                    if (fields.put(SECONDARY_BIT_MAP_INDEX, bf) == null) {
                        length += bf.length;
                    }
                } else {
                    final ISOField bf = getPackager().packBitmap(tertiaryBitMap, TERTIARY_BIT_MAP_INDEX);
                    if (fields.put(TERTIARY_BIT_MAP_INDEX, bf) == null) {
                        length += bf.length;
                    }
                }
            }
        }
    }
    
    public final boolean hasField(final int index) {
        return fields.containsKey(index);
    }
    
    public final SortedMap<Integer, ISOField> getFields() {
        return Collections.unmodifiableSortedMap(fields);
    }
    
    public static final class ISOField {
        
        public final int length;
        public final UnmodifiableByteArray data;
        public final byte[] rawData;
        
        public ISOField(final int length, final byte[] rawData) {
            this.length = length;
            this.rawData = rawData;
            this.data = new UnmodifiableByteArray(rawData);
        }
        
        public String toString(final Charset charset) {
            return charset != null ? new String(rawData, charset) : new String(rawData);
        }
        
        public void write(final OutputStream os) throws IOException {
            os.write(rawData);
        }
        
    }
    
}

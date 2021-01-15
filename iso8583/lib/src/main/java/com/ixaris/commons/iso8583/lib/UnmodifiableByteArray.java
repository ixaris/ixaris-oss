package com.ixaris.commons.iso8583.lib;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class UnmodifiableByteArray implements Iterable<Byte> {
    
    private final byte[] a;
    
    public UnmodifiableByteArray(final byte[] a) {
        this.a = a;
    }
    
    public int getLength() {
        return a.length;
    }
    
    public byte get(final int i) {
        return a[i];
    }
    
    public InputStream getInputStream() {
        return new ByteArrayInputStream(a);
    }
    
    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {
            
            private int i = 0;
            
            @Override
            public Byte next() {
                if (i >= a.length) {
                    throw new NoSuchElementException();
                }
                return a[i++];
            }
            
            @Override
            public boolean hasNext() {
                return i < a.length;
            }
        };
    }
    
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (!(o instanceof UnmodifiableByteArray)) {
            return false;
        } else {
            final UnmodifiableByteArray other = (UnmodifiableByteArray) o;
            return Arrays.equals(a, other.a);
        }
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(a);
    }
}

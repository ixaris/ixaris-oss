package com.ixaris.commons.collections.lib;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

import com.ixaris.commons.misc.lib.object.Wrapper;

public abstract class BitSet {
    
    private static final BitSet EMPTY = new Empty();
    
    public static BitSet empty() {
        return EMPTY;
    }
    
    public static BitSet unmodifiable(final BitSet wrapped) {
        return (wrapped instanceof Unmodifiable) ? wrapped : new Unmodifiable(wrapped);
    }
    
    public static BitSet of(final int max, int... values) {
        return new Impl(max, values);
    }
    
    public static BitSet of(final int max, final Collection<Integer> collection) {
        return new Impl(max, collection.stream().mapToInt(i -> i).toArray());
    }
    
    public static BitSet copy(final BitSet set) {
        return new Impl(set);
    }
    
    @SuppressWarnings("squid:S2972")
    private static final class Empty extends BitSet {
        
        private static final int[] EMPTY_ARRAY = new int[0];
        private static final Integer[] EMPTY_BOXED_ARRAY = new Integer[0];
        
        @Override
        public int getMax() {
            return 0;
        }
        
        @Override
        public int size() {
            return 0;
        }
        
        @Override
        public boolean isEmpty() {
            return true;
        }
        
        @Override
        public boolean contains(final int i) {
            return false;
        }
        
        @Override
        public BitSetIterator iterator() {
            return new BitSetIterator() {
                
                @Override
                public boolean hasNext() {
                    return false;
                }
                
                @Override
                public int next() {
                    throw new NoSuchElementException();
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
            };
        }
        
        @Override
        public boolean add(final int i) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void addAll(final BitSet set) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean remove(final int i) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void removeAll(final BitSet set) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int[] toArray() {
            return EMPTY_ARRAY;
        }
        
        @Override
        public Integer[] toBoxedArray() {
            return EMPTY_BOXED_ARRAY;
        }
        
        @Override
        @SuppressWarnings("squid:S2162")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof BitSet) {
                final BitSet other = (BitSet) o;
                return other.isEmpty();
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return 0;
        }
        
        @Override
        public String toString() {
            return "[]";
        }
    }
    
    @SuppressWarnings("squid:S2972")
    private static final class Unmodifiable extends BitSet implements Wrapper<BitSet> {
        
        private final BitSet wrapped;
        
        public Unmodifiable(final BitSet wrapped) {
            if (wrapped == null) {
                throw new IllegalArgumentException("wrapped is null");
            }
            this.wrapped = wrapped;
        }
        
        @Override
        public int getMax() {
            return wrapped.getMax();
        }
        
        @Override
        public int size() {
            return wrapped.size();
        }
        
        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }
        
        @Override
        public boolean contains(final int i) {
            return wrapped.contains(i);
        }
        
        @Override
        public BitSetIterator iterator() {
            return new BitSetIterator.Unmodifiable(wrapped.iterator());
        }
        
        @Override
        public boolean add(final int i) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void addAll(final BitSet set) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean remove(final int i) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void removeAll(final BitSet set) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public int[] toArray() {
            return wrapped.toArray();
        }
        
        @Override
        public Integer[] toBoxedArray() {
            return wrapped.toBoxedArray();
        }
        
        @Override
        public BitSet unwrap() {
            return wrapped;
        }
        
        @Override
        @SuppressWarnings("squid:S2162")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Unmodifiable) {
                final Unmodifiable other = (Unmodifiable) o;
                return wrapped.equals(other.wrapped);
            }
            if (o instanceof BitSet) {
                final BitSet other = (BitSet) o;
                return wrapped.equals(other);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }
        
        @Override
        public String toString() {
            return wrapped.toString();
        }
        
    }
    
    /**
     * Set based on bitmasks, each long containing 64 numbers. To ass a number in the set, set the bit in the
     * appropriate bitmask.
     */
    @SuppressWarnings("squid:S2972")
    private static final class Impl extends BitSet {
        
        private int size = 0;
        private final int max;
        private final long[] bitmask;
        
        public Impl(final int max, int... values) {
            if (max < 0) {
                throw new IllegalArgumentException();
            }
            this.max = max;
            bitmask = new long[(max + 63) >>> 6];
            for (final int i : values) {
                add(i);
            }
        }
        
        public Impl(final BitSet set) {
            this.max = set.getMax();
            if (set instanceof Impl) {
                final Impl other = (Impl) set;
                this.size = other.size;
                bitmask = Arrays.copyOf(other.bitmask, other.bitmask.length);
            } else {
                bitmask = new long[(max + 63) >>> 6];
                addAll(set);
            }
        }
        
        @Override
        public int getMax() {
            return max;
        }
        
        @Override
        public int size() {
            return size;
        }
        
        @Override
        public boolean isEmpty() {
            return size == 0;
        }
        
        @Override
        public boolean contains(final int i) {
            rangeCheck(i);
            return internalContains(i);
        }
        
        private boolean internalContains(final int i) {
            return (bitmask[i >>> 6] & (1L << (i & 0x3F))) != 0;
        }
        
        @Override
        @SuppressWarnings("squid:S1171")
        public BitSetIterator iterator() {
            return new BitSetIterator() {
                
                private int cur = -1;
                private int next = -1;
                
                {
                    findNext();
                }
                
                @Override
                public boolean hasNext() {
                    return next < max;
                }
                
                @Override
                public int next() {
                    if (next < max) {
                        cur = next;
                        findNext();
                        return cur;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
                
                @Override
                public void remove() {
                    Impl.this.remove(cur);
                }
                
                private void findNext() {
                    do {
                        next++;
                    } while ((next < max) && !contains(next));
                }
                
            };
        }
        
        @Override
        public boolean add(final int i) {
            rangeCheck(i);
            final int indexInBitset = i >>> 6;
            final long prev = bitmask[indexInBitset];
            final long cur = prev | (1L << (i & 0x3F));
            if (cur != prev) {
                bitmask[indexInBitset] = cur;
                size++;
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public boolean remove(final int i) {
            rangeCheck(i);
            final int indexInBitset = i >>> 6;
            final long prev = bitmask[indexInBitset];
            final long cur = prev & ~(1L << (i & 0x3F));
            if (cur != prev) {
                bitmask[indexInBitset] = cur;
                size--;
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public void clear() {
            size = 0;
            Arrays.fill(bitmask, 0);
        }
        
        @Override
        @SuppressWarnings("squid:S134")
        public int[] toArray() {
            final int[] result = new int[size];
            int pos = 0;
            int add = 0;
            for (final long cur : bitmask) {
                if (cur != 0) {
                    for (int j = 0; j < 64; j++) {
                        if ((cur & (1L << j)) != 0L) {
                            result[pos] = j + add;
                            pos++;
                        }
                    }
                }
                add = add + 64;
            }
            return result;
        }
        
        @Override
        @SuppressWarnings("squid:S134")
        public Integer[] toBoxedArray() {
            final Integer[] result = new Integer[size];
            int pos = 0;
            int add = 0;
            for (final long cur : bitmask) {
                if (cur != 0) {
                    for (int j = 0; j < 64; j++) {
                        if ((cur & (1L << j)) != 0L) {
                            result[pos] = j + add;
                            pos++;
                        }
                    }
                }
                add = add + 64;
            }
            return result;
        }
        
        @Override
        @SuppressWarnings("squid:S2162")
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof Impl) {
                final Impl other = (Impl) o;
                return max == other.max && Arrays.equals(bitmask, other.bitmask);
            }
            if (o instanceof BitSet) {
                final BitSet other = (BitSet) o;
                return Arrays.equals(toArray(), other.toArray());
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return Arrays.hashCode(bitmask);
        }
        
        @Override
        public String toString() {
            return Arrays.toString(toArray());
        }
        
        private void rangeCheck(final int i) {
            if ((i < 0) || (i >= max)) {
                throw new IndexOutOfBoundsException(String.format("Range is 0 - %d, given %d", max - 1, i));
            }
        }
        
    }
    
    public abstract int getMax();
    
    public abstract int size();
    
    public abstract boolean isEmpty();
    
    public abstract boolean contains(int i);
    
    public abstract BitSetIterator iterator();
    
    /**
     * Add an item to the set. Returns <tt>true</tt> if the set changed as a result
     */
    public abstract boolean add(int i);
    
    public void addAll(final BitSet set) {
        for (final BitSetIterator i = set.iterator(); i.hasNext();) {
            add(i.next());
        }
    }
    
    /**
     * Remove an item from the set. Returns <tt>true</tt> if the set changed as a result
     */
    public abstract boolean remove(int i);
    
    public void removeAll(final BitSet set) {
        for (final BitSetIterator i = set.iterator(); i.hasNext();) {
            remove(i.next());
        }
    }
    
    public abstract void clear();
    
    public abstract int[] toArray();
    
    public abstract Integer[] toBoxedArray();
    
}

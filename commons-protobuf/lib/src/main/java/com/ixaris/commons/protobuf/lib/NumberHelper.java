package com.ixaris.commons.protobuf.lib;

public final class NumberHelper {
    
    public static boolean isDefault(final Number number) {
        if (number instanceof Integer) {
            return number.intValue() == 0;
        } else if (number instanceof Long) {
            return number.longValue() == 0L;
        } else if (number instanceof Float) {
            return Float.compare(number.floatValue(), 0.0f) == 0;
        } else if (number instanceof Double) {
            return Double.compare(number.doubleValue(), 0.0) == 0;
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    public static int compare(final Number n1, final Number n2) {
        if (n1 instanceof Integer) {
            if (n2 instanceof Integer) {
                return Integer.compare(n1.intValue(), n2.intValue());
            } else if (n2 instanceof Long) {
                return Long.compare(n1.longValue(), n2.longValue());
            } else if (n2 instanceof Float) {
                return Float.compare(n1.floatValue(), n2.floatValue());
            } else if (n2 instanceof Double) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (n1 instanceof Long) {
            if ((n2 instanceof Integer) || (n2 instanceof Long)) {
                return Long.compare(n1.longValue(), n2.longValue());
            } else if (n2 instanceof Float) {
                return Float.compare(n1.floatValue(), n2.floatValue());
            } else if (n2 instanceof Double) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (n1 instanceof Float) {
            if ((n2 instanceof Integer) || (n2 instanceof Long) || (n2 instanceof Float)) {
                return Float.compare(n1.floatValue(), n2.floatValue());
            } else if (n2 instanceof Double) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (n1 instanceof Double) {
            if ((n2 instanceof Integer) || (n2 instanceof Long) || (n2 instanceof Float) || (n2 instanceof Double)) {
                return Double.compare(n1.doubleValue(), n2.doubleValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    private NumberHelper() {}
    
}

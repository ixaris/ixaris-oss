/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import java.text.DateFormat;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Timestamp value object
 *
 * @author <a href="mailto:olivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public abstract class AbstractDateValue extends Value implements Comparable<AbstractDateValue> {
    
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);
    
    public abstract static class Builder<T extends AbstractDateValue> implements Value.Builder<T> {
        
        protected abstract T build(long value);
        
        @Override
        public final int getNumberOfParts() {
            return 3;
        }
        
        @Override
        public final boolean isPartFixedValue(final int part) {
            return true;
        }
        
        @Override
        public final Map<String, String> getPartFixedValues(final int part) {
            if (part == 0) {
                final Map<String, String> days = new LinkedHashMap<>(31);
                for (int day = 1; day <= 31; day++) {
                    days.put(Integer.toString(day), Integer.toString(day));
                }
                return days;
                
            } else if (part == 1) {
                final Map<String, String> months = new LinkedHashMap<>(12);
                for (final Month month : Month.values()) {
                    months.put(month.name(), month.name());
                }
                
                return months;
                
            } else if (part == 2) {
                // years from 2010 to 5 years past the current year
                int currentYear = ZonedDateTime.now(ZoneOffset.UTC).getYear();
                int totalYears = currentYear - 2010 + 5;
                
                final Map<String, String> years = new LinkedHashMap<>(totalYears);
                for (int year = 2010; year <= 2010 + totalYears; year++) {
                    years.put(Integer.toString(year), Integer.toString(year));
                }
                return years;
                
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        @Override
        public final T buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getLongValue() == null) {
                throw new IllegalArgumentException("Long part is null");
            }
            
            return build(persistedValue.getLongValue());
        }
        
        @Override
        public final T buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 3) {
                throw new IllegalArgumentException("Should have 3 parts. Found " + parts.length);
            }
            if (parts[0] == null || parts[1] == null || parts[2] == null) {
                throw new IllegalArgumentException("A part is null");
            }
            
            final ZonedDateTime date = ZonedDateTime.of(Integer.parseInt(parts[2]),
                Month.valueOf(parts[1]).ordinal() + 1,
                Integer.parseInt(parts[0]),
                0,
                0,
                0,
                0,
                ZoneOffset.UTC);
            return build(date.toInstant().toEpochMilli());
        }
    }
    
    enum DateAdjustment {
        
        START_OF_DAY {
            
            @Override
            public ZonedDateTime adjust(final ZonedDateTime date) {
                return date.withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
        },
        END_OF_DAY {
            
            @Override
            public ZonedDateTime adjust(final ZonedDateTime date) {
                return date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            }
        };
        
        public abstract ZonedDateTime adjust(final ZonedDateTime value);
    }
    
    protected final long value;
    
    public AbstractDateValue(final long value) {
        final ZonedDateTime date = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
        this.value = getAdjustment().adjust(date).toInstant().toEpochMilli();
    }
    
    protected abstract DateAdjustment getAdjustment();
    
    @Override
    public final String[] getStringParts() {
        final ZonedDateTime date = getValue();
        return new String[] { Integer.toString(date.getDayOfMonth()), date.getMonth().name(), Integer.toString(date.getYear()) };
    }
    
    public final ZonedDateTime getValue() {
        return Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
    }
    
    public final Date getDate() {
        return new Date(value);
    }
    
    @Override
    public final String toString() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(getDate());
        }
    }
    
    @Override
    public final int compareTo(final AbstractDateValue o) {
        return Long.compare(value, o.value);
    }
    
    @Override
    public final PersistedValue getPersistedValue() {
        return new PersistedValue(this.value, null);
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value == other.value);
    }
    
    @Override
    public final int hashCode() {
        return Long.hashCode(value);
    }
    
}

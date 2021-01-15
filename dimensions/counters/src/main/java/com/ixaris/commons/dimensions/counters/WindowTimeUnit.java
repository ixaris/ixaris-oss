/*
 * Copyright 2002 - 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The Unit of Time enumeration used for Window size.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public enum WindowTimeUnit {
    
    MINUTE(60) {
        
        @Override
        public long getStartTimestamp(long timestamp, int windowWidthAmount) {
            final long millisInWidth = MILLISECONDS_IN_MINUTE * windowWidthAmount;
            return (timestamp / millisInWidth) * millisInWidth;
        }
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            return timestamp / (MILLISECONDS_IN_MINUTE * windowWidthAmount);
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            return windowNumber * (MILLISECONDS_IN_MINUTE * windowWidthAmount);
        }
        
    },
    
    HOUR(24) {
        
        @Override
        public long getStartTimestamp(long timestamp, int windowWidthAmount) {
            final long millisInWidth = MILLISECONDS_IN_HOUR * windowWidthAmount;
            return (timestamp / millisInWidth) * millisInWidth;
        }
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            return timestamp / (MILLISECONDS_IN_HOUR * windowWidthAmount);
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            return windowNumber * (MILLISECONDS_IN_HOUR * windowWidthAmount);
        }
        
    },
    
    DAY(180) {
        
        @Override
        public long getStartTimestamp(long timestamp, int windowWidthAmount) {
            final long millisInWidth = MILLISECONDS_IN_DAY * windowWidthAmount;
            return (timestamp / millisInWidth) * millisInWidth;
        }
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            return timestamp / (MILLISECONDS_IN_DAY * windowWidthAmount);
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            return windowNumber * (MILLISECONDS_IN_DAY * windowWidthAmount);
        }
        
    },
    
    WEEK(26) {
        
        @Override
        public long getStartTimestamp(long timestamp, int windowWidthAmount) {
            final long millisInWidth = MILLISECONDS_IN_WEEK * windowWidthAmount;
            return (timestamp / millisInWidth) * millisInWidth;
        }
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            return timestamp / (MILLISECONDS_IN_WEEK * windowWidthAmount);
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            return windowNumber * (MILLISECONDS_IN_WEEK * windowWidthAmount);
        }
        
    },
    
    MONTH(24) {
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            final ZonedDateTime dateTime = ZonedDateTime.from(Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC));
            return (((dateTime.getYear() - 1970) * 12) + (dateTime.getMonthValue() - 1)) / windowWidthAmount;
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            final long monthNumber = windowNumber * windowWidthAmount;
            int yearNumber = (int) monthNumber / 12;
            int monthInYear = (int) (monthNumber - (yearNumber * 12));
            return ZonedDateTime
                .of(yearNumber + 1970, monthInYear + 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                .toEpochSecond() * 1000L;
        }
        
    },
    
    YEAR(5) {
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            final ZonedDateTime dateTime = ZonedDateTime.from(Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC));
            return (dateTime.getYear() - 1970) / windowWidthAmount;
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            int year = (int) windowNumber * windowWidthAmount;
            return ZonedDateTime
                .of(year + 1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                .toEpochSecond() * 1000L;
        }
        
    },
    
    /**
     * ALWAYS time unit represent window-less counters.
     *
     * <p><b>Note that this time unit must <em>only</em> be used in restricted cases where archiving is not applicable.</b>
     */
    ALWAYS(1) {
        
        @Override
        public long getWindowNumber(final long timestamp, final int windowWidthAmount) {
            return 1;
        }
        
        @Override
        public long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount) {
            return 0;
        }
        
        @Override
        public boolean isValid(final int amount) {
            return true;
        }
        
    };
    
    public static final long MILLISECONDS_IN_MINUTE = 60000;
    public static final long MILLISECONDS_IN_HOUR = MILLISECONDS_IN_MINUTE * 60;
    public static final long MILLISECONDS_IN_DAY = MILLISECONDS_IN_HOUR * 24;
    public static final long MILLISECONDS_IN_WEEK = MILLISECONDS_IN_DAY * 7;
    
    /**
     * Maximum window-width-amount allowed by the time unit (exclusive). In some cases only factors of this value are allowed.
     */
    private final int maxValue;
    
    WindowTimeUnit(final int maxValue) {
        this.maxValue = maxValue;
    }
    
    /**
     * Get the start time of window for a specific time and window size. The implementation align windows to 1st January 1970 00:00:00 UTC time.
     *
     * @param timestamp
     * @param windowWidthAmount
     * @return
     */
    public long getStartTimestamp(final long timestamp, final int windowWidthAmount) {
        return getStartTimestampForWindowNumber(getWindowNumber(timestamp, windowWidthAmount), windowWidthAmount);
    }
    
    /**
     * Returns a unique number to a particular timestamp and window width. The number is idempotent and consecutive for adjacent windows. Windows
     * are aligned to 1st January 1970 00:00:00 UTC time, and the returned value is the window number since that date.
     *
     * @param timestamp
     * @param windowWidthAmount
     * @return
     */
    public abstract long getWindowNumber(final long timestamp, final int windowWidthAmount);
    
    public abstract long getStartTimestampForWindowNumber(final long windowNumber, final int windowWidthAmount);
    
    /**
     * @return Maximum value allowed for a unit
     */
    public int getMaxValue() {
        return maxValue - 1;
    }
    
    /**
     * @param amount
     * @return True if amount is smaller than maxAmount, false otherwise.
     */
    public boolean isValid(final int amount) {
        return amount < maxValue;
    }
    
}

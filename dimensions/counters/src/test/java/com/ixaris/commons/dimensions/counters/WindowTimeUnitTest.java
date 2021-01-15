package com.ixaris.commons.dimensions.counters;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowTimeUnitTest {
    
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    
    @Test
    public void startTimeMinuteTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 1, 1, 1, 5);
        
        // testing round to nearest 5 minutes
        calCurrent.set(2000, 1, 1, 1, 5);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
        calCurrent.set(2000, 1, 1, 1, 6);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
        calCurrent.set(2000, 1, 1, 1, 7);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
        calCurrent.set(2000, 1, 1, 1, 8);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
        calCurrent.set(2000, 1, 1, 1, 9);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
        calCurrent.set(2000, 1, 1, 1, 10);
        calStart.set(2000, 1, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MINUTE.getStartTimestamp(calCurrent.getTimeInMillis(), 5));
    }
    
    @Test
    public void startTimeHourTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 1, 1, 0, 0);
        
        // testing round to nearest 4 hours
        calCurrent.set(2000, 1, 1, 0, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.HOUR.getStartTimestamp(calCurrent.getTimeInMillis(), 4));
        calCurrent.set(2000, 1, 1, 1, 20);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.HOUR.getStartTimestamp(calCurrent.getTimeInMillis(), 4));
        calCurrent.set(2000, 1, 1, 2, 20);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.HOUR.getStartTimestamp(calCurrent.getTimeInMillis(), 4));
        calCurrent.set(2000, 1, 1, 3, 20);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.HOUR.getStartTimestamp(calCurrent.getTimeInMillis(), 4));
        calCurrent.set(2000, 1, 1, 4, 20);
        calStart.set(2000, 1, 1, 4, 0);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.HOUR.getStartTimestamp(calCurrent.getTimeInMillis(), 4));
    }
    
    @Test
    public void startTimeDayTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 1, 2);
        
        // testing round to nearest 3 days
        calCurrent.set(2000, 1, 2, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.DAY.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 1, 3, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.DAY.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 1, 4, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.DAY.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 1, 5, 1, 10);
        calStart.set(2000, 1, 5);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.DAY.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
    }
    
    @Test
    public void startTimeWeekTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 0, 20);
        
        // testing round to nearest 2 weeks
        calCurrent.set(2000, 0, 20, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.WEEK.getStartTimestamp(calCurrent.getTimeInMillis(), 2));
        for (int i = 0; i < 13; i++) {
            calCurrent.add(Calendar.DAY_OF_MONTH, 1);
            assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.WEEK.getStartTimestamp(calCurrent.getTimeInMillis(), 2));
        }
        calCurrent.add(Calendar.DAY_OF_MONTH, 1);
        calStart.add(Calendar.DAY_OF_MONTH, 14);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.WEEK.getStartTimestamp(calCurrent.getTimeInMillis(), 2));
    }
    
    @Test
    public void startTimeMonthTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 0, 1);
        
        // testing round to nearest 3 months
        calCurrent.set(2000, 0, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MONTH.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 1, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MONTH.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 2, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MONTH.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2000, 3, 1, 1, 10);
        calStart.set(2000, 3, 1);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.MONTH.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
    }
    
    @Test
    public void startTimeYearTest() {
        Calendar calCurrent = Calendar.getInstance(UTC);
        Calendar calStart = Calendar.getInstance(UTC);
        calStart.clear();
        calStart.set(2000, 0, 1);
        
        // testing round to nearest 3 years
        calCurrent.set(2000, 2, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.YEAR.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2001, 2, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.YEAR.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2002, 2, 1, 1, 10);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.YEAR.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
        calCurrent.set(2003, 2, 1, 1, 10);
        calStart.set(2003, 0, 1);
        assertEquals(calStart.getTimeInMillis(), WindowTimeUnit.YEAR.getStartTimestamp(calCurrent.getTimeInMillis(), 3));
    }
    
    @Test
    public void testMinute() {
        // test window sizes 1 - 50 minutes
        for (int windowsize = 1; windowsize < 24; windowsize++) {
            
            // test different minutes of hour
            for (int offset = 0; offset < 40; offset++) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.MINUTE, offset);
                
                long prevWindowNumber = WindowTimeUnit.MINUTE.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 100; i++) { // for the next 100 windows
                    cal.add(GregorianCalendar.MINUTE, windowsize);
                    final long windowNumber = WindowTimeUnit.MINUTE.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.MINUTE.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.MINUTE.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void testHour() {
        // test window sizes 1 - 23 hours
        for (int windowsize = 1; windowsize < 24; windowsize++) {
            
            // test different hours of day
            for (int offset = 0; offset < 14; offset++) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.HOUR, offset);
                
                long prevWindowNumber = WindowTimeUnit.HOUR.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 100; i++) { // for the next 100 windows
                    cal.add(GregorianCalendar.HOUR, windowsize);
                    final long windowNumber = WindowTimeUnit.HOUR.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.HOUR.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.HOUR.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void testDay() {
        // test window sizes 1 - 100 days
        for (int windowsize = 1; windowsize < 100; windowsize++) {
            
            // test different days in months
            for (int offset = 0; offset < 30; offset++) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.DAY_OF_YEAR, offset);
                
                long prevWindowNumber = WindowTimeUnit.DAY.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 100; i++) { // for the next 100 windows
                    cal.add(GregorianCalendar.DAY_OF_YEAR, windowsize);
                    final long windowNumber = WindowTimeUnit.DAY.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.DAY.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.DAY.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void testWeek() {
        // test window sizes 1 - 6 weeks
        for (int windowsize = 1; windowsize < 7; windowsize++) {
            
            // test each day of week
            for (int offset = 0; offset < 7; offset++) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.DAY_OF_MONTH, offset);
                
                long prevWindowNumber = WindowTimeUnit.WEEK.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 10 * 52; i++) { // for the next 10 years * windowsize
                    cal.add(GregorianCalendar.DAY_OF_MONTH, windowsize * 7);
                    final long windowNumber = WindowTimeUnit.WEEK.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.WEEK.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.WEEK.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void testMonth() {
        // test window sizes 1 - 4 months
        for (int windowsize = 1; windowsize < 5; windowsize++) {
            
            // test different days in months
            for (int offset = 0; offset < 28; offset++) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.DAY_OF_MONTH, offset);
                
                long prevWindowNumber = WindowTimeUnit.MONTH.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 100; i++) { // for the next 100 windows
                    cal.add(GregorianCalendar.MONTH, windowsize);
                    final long windowNumber = WindowTimeUnit.MONTH.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.MONTH.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.MONTH.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void testYear() {
        // test window sizes 1 - 5 years
        for (int windowsize = 3; windowsize < 6; windowsize++) {
            
            // test different days / months
            for (int offset = 0; offset < (360 / windowsize); offset += 13) {
                
                GregorianCalendar cal = new GregorianCalendar(UTC);
                cal.add(GregorianCalendar.DAY_OF_MONTH, offset);
                
                long prevWindowNumber = WindowTimeUnit.YEAR.getWindowNumber(cal.getTimeInMillis(), windowsize);
                for (int i = 0; i < 100; i++) { // for the next 100 windows
                    cal.add(GregorianCalendar.YEAR, windowsize);
                    final long windowNumber = WindowTimeUnit.YEAR.getWindowNumber(cal.getTimeInMillis(), windowsize);
                    assertEquals(++prevWindowNumber, windowNumber);
                    assertEquals(WindowTimeUnit.YEAR.getStartTimestamp(cal.getTimeInMillis(), windowsize),
                        WindowTimeUnit.YEAR.getStartTimestampForWindowNumber(windowNumber, windowsize));
                }
            }
        }
    }
    
    @Test
    public void relativeWindowNumberTest() {
        long now = System.currentTimeMillis();
        long minuteWindowNumber = WindowTimeUnit.MINUTE.getWindowNumber(now, 1);
        
        long hourWindowNumber = WindowTimeUnit.HOUR.getWindowNumber(now, 1);
        assertTrue("minuteWindowNumber [" + minuteWindowNumber + "] must be >= hourWindowNumber * 60 [" + hourWindowNumber * 60 + "]",
            minuteWindowNumber >= hourWindowNumber * 60);
        assertTrue("minuteWindowNumber [" + minuteWindowNumber + "] must be <= hourWindowNumber+1 * 60 [" + (hourWindowNumber + 1) * 60 + "]",
            minuteWindowNumber <= (hourWindowNumber + 1) * 60);
        
        long dayWindowNumber = WindowTimeUnit.DAY.getWindowNumber(now, 1);
        assertTrue("hourWindowNumber [" + hourWindowNumber + "] must be >= dayWindowNumber * 24 [" + dayWindowNumber * 24 + "]",
            hourWindowNumber >= dayWindowNumber * 24);
        assertTrue("hourWindowNumber [" + hourWindowNumber + "] must be <= dayWindowNumber+1 * 24 [" + (dayWindowNumber + 1) * 24 + "]",
            hourWindowNumber <= (dayWindowNumber + 1) * 24);
        
        long weekWindowNumber = WindowTimeUnit.WEEK.getWindowNumber(now, 1);
        assertTrue("dayWindowNumber [" + dayWindowNumber + "] must be >= weekWindowNumber * 7 [" + weekWindowNumber + "]",
            dayWindowNumber >= weekWindowNumber * 7);
        assertTrue("dayWindowNumber [" + dayWindowNumber + "] must be <= weekWindowNumber+1 * 7 [" + (weekWindowNumber + 1) * 7 + "]",
            dayWindowNumber <= (weekWindowNumber + 1) * 7);
        
        long monthWindowNumber = WindowTimeUnit.MONTH.getWindowNumber(now, 1);
        long yearWindowNumber = WindowTimeUnit.YEAR.getWindowNumber(now, 1);
        assertTrue("monthWindowNumber [" + monthWindowNumber + "] must be >= yearWindowNumber * 12 [" + yearWindowNumber * 12 + "]",
            monthWindowNumber >= yearWindowNumber * 12);
        assertTrue("monthWindowNumber [" + monthWindowNumber + "] must be <= yearWindowNumber+1 * 12 [" + (yearWindowNumber + 1) * 12 + "]",
            monthWindowNumber <= (yearWindowNumber + 1) * 12);
    }
    
}

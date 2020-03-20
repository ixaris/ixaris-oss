package com.ixaris.commons.misc.lib.conversion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SnakeCaseHelper {
    
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?:_)([^_])");
    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([A-Z])");
    
    public static String snakeToCamelCase(final String s) {
        if ((s == null) || s.isEmpty()) {
            return s;
        }
        final Matcher m = SNAKE_CASE_PATTERN.matcher(s);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public static String camelToSnakeCase(final String s) {
        if ((s == null) || s.isEmpty()) {
            return s;
        }
        final Matcher m = CAMEL_CASE_PATTERN.matcher(s);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "_" + m.group(1).toLowerCase());
        }
        m.appendTail(sb);
        if (sb.length() > 1 && sb.charAt(0) == '_') {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }
    
    public static String snakeToHuman(final String s) {
        return capitalize(s.replaceAll("_", " "));
    }
    
    // Copied from WordUtils to avoid adding a dependency for just a single method
    private static String capitalize(final String s) {
        if ((s == null) || s.isEmpty()) {
            return s;
        }
        final char[] buffer = s.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }
    
    private SnakeCaseHelper() {}
    
}

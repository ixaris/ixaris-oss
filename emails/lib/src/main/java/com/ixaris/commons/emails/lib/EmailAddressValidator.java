package com.ixaris.commons.emails.lib;

import java.util.regex.Pattern;

/**
 * Helper class that can be used to check if a string contains a valid email address.
 *
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public final class EmailAddressValidator {
    
    /**
     * Regular expression for our definition of a valid email address. Note from a security perspective: We typically try to avoid these
     * characters: ` ' / \ " < > = spaces. These can be typically also used for malicious purposes, hence shouldn't be accepted anywhere in clear
     * text. There are also other restrictions which are typically best practice for emails, such as the dot (.) is accepted provided that it is
     * not the first or last character, and provided also that it does not appear two or more times consecutively.
     */
    public static final String EMAIL_ADDRESS_REGEX = "^([a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+=?^_`{|}~-]+)*@[a-zA-Z0-9_\\-+=&&[^//]]+(\\.[a-zA-Z0-9_\\-+=&&[^//]]+)+)$";
    
    /**
     * Pre-compiled regex pattern for our definition of a valid email address.
     */
    public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(EMAIL_ADDRESS_REGEX);
    
    /**
     * Tests a string to determine whether it contains a valid email address by attempting to match it to {@link
     * EmailAddressValidator#EMAIL_ADDRESS_REGEX}.
     *
     * @param potentialEmailAddress String being tested.
     * @return True if the given string, {@code potentialEmailAddress}, contains a valid email address. False if {@code potentialEmailAddress} is
     *     null or is not a valid email address.
     */
    public static boolean isValidEmailAddress(final String potentialEmailAddress) {
        if (potentialEmailAddress == null) {
            return false;
        }
        
        return EMAIL_ADDRESS_PATTERN.matcher(potentialEmailAddress).matches();
    }
    
    private EmailAddressValidator() {}
    
}

package com.ixaris.commons.emails.lib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for the type {@link EmailAddressValidator}.
 *
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public class EmailAddressValidatorTest {
    
    @Test
    public void isValidEmailAddress_SimpleValidAddress_ReturnValidAddress() {
        assertTrue(EmailAddressValidator.isValidEmailAddress("test@ixaris.com"));
    }
    
    @Test
    public void isValidEmailAddress_ValidAddressContainingAllowedSymbols_ReturnValidAddress() {
        assertTrue(EmailAddressValidator.isValidEmailAddress("te.st@i-x_a+ris.com"));
    }
    
    @Test
    public void isValidEmailAddress_EmptyString_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress(""));
    }
    
    @Test
    public void isValidEmailAddress_Null_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress(null));
    }
    
    @Test
    public void isValidEmailAddress_AddressStartsWithFullstop_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress(".test@ixaris.com"));
    }
    
    @Test
    public void isValidEmailAddress_AddressContainsConsecutiveFullstopsBeforeAtSymbol_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress("te..st@ixaris.com"));
    }
    
    @Test
    public void isValidEmailAddress_AddressContainsConsecutiveFullstopsAfterAtSymbol_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress("test@ixa..ris.com"));
    }
    
    @Test
    public void isValidEmailAddress_AddressContainsSpaceBeforeAtSymbol_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress("te st@ixaris.com"));
    }
    
    @Test
    public void isValidEmailAddress_AddressContainsSpaceAfterAtSymbol_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress("test@ixa ris.com"));
    }
    
    @Test
    public void isValidEmailAddress_AddressMissingFinalSection_ReturnInvalidAddress() {
        assertFalse(EmailAddressValidator.isValidEmailAddress("test@ixaris"));
    }
    
}

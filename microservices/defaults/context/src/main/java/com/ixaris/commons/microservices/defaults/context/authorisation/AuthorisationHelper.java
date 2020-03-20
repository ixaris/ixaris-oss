package com.ixaris.commons.microservices.defaults.context.authorisation;

import static com.ixaris.commons.microservices.defaults.context.AsyncLocals.SUBJECT;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.SubjectOrBuilder;

/**
 * Helper class aimed at reducing duplication of recurring authorisation rules
 *
 * @author <a href="keith.spiteri@ixaris.com">keith.spiteri</a>
 */
public final class AuthorisationHelper {
    
    public static boolean isAuthorised(final boolean isOwner) {
        return isAdmin(SUBJECT.get()) || isOwner;
    }
    
    public static boolean isAdmin(final SubjectOrBuilder subject) {
        // The user group can only be obtained from the admin_info spi, so it is assumed that
        // if you have a user group or are a super user, then you are an admin.
        return !subject.getUserGroupId().isEmpty() || subject.getIsSuperUser();
    }
    
    private AuthorisationHelper() {}
    
}

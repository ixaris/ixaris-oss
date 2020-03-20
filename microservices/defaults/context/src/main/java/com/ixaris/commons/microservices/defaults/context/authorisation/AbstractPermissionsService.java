package com.ixaris.commons.microservices.defaults.context.authorisation;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;

/**
 * Abstraction of a service that grants and checks permissions for {{@link Subject}}s. Permission checks are based on user groups, and have
 * exceptional cases for super users.
 *
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public abstract class AbstractPermissionsService<PERMISSION> {
    
    public static final char USER_GROUP_SEPARATOR = '_';
    
    /**
     * Grant the given permission to a target user group. Can only grant permissions that the current subject has
     *
     * @param granter The subject that is granting the permission
     * @param targetUserGroupId The user group that will be given the new permission
     * @param permission The permission being granted
     * @throws UnauthorisedException if the subject is not authorised to grant the permission
     */
    public final void grantPermission(final Subject granter, final String targetUserGroupId, final PERMISSION permission) throws UnauthorisedException {
        if (!canGrantToGroup(granter, targetUserGroupId)) {
            throw new UnauthorisedException(granter.getCredentialId()
                + " of type "
                + granter.getCredentialType()
                + " is trying to grant a "
                + permission
                + " to "
                + targetUserGroupId
                + " but is not in a parent user group");
        }
        if (!hasPermission(granter, permission)) {
            throw new UnauthorisedException(granter.getCredentialId()
                + " of type "
                + granter.getCredentialType()
                + " is trying to grant a "
                + permission
                + " to "
                + targetUserGroupId
                + " but does not have the permission being granted");
        }
        
        grantPermission(targetUserGroupId, permission);
    }
    
    /**
     * Revoke the given permission from a target user group.
     *
     * @param granter The subject that is revoking the permission
     * @param targetUserGroupId The user group from which the permission is to be revoked
     * @param permission The permission being revoked
     * @throws UnauthorisedException if the subject is not authorised to revoke the permission
     */
    public final void revokePermission(final Subject granter, final String targetUserGroupId, final PERMISSION permission) throws UnauthorisedException {
        if (!canGrantToGroup(granter, targetUserGroupId)) {
            throw new UnauthorisedException(granter.getCredentialId()
                + " of type "
                + granter.getCredentialType()
                + " is trying to grant a "
                + permission
                + " to "
                + targetUserGroupId
                + " but is not in a parent user group");
        }
        
        revokePermission(targetUserGroupId, permission);
    }
    
    /**
     * Check whether a subject has a given permission.
     *
     * @param subject The subject of the permission check.
     * @param permission The permission being checked.
     * @return true if the subject is a super user or is in a user group that has the permission
     */
    public final boolean hasPermission(final Subject subject, final PERMISSION permission) {
        return subject.getIsSuperUser() || hasPermission(subject.getUserGroupId(), permission);
    }
    
    /**
     * Check whether a subject can grant permissions to the given target group id. Default implementation allows superuser to grant to anyone,
     * otherwise checks that the targetGroupId is prefixed with the granter's userGroupId + USER_GROUP_SEPARATOR
     *
     * <p>e.g. given '_' as a separator, granter in group 'group1' can grant to group 'group1_1' and 'group1_2' but not to 'group10'
     *
     * @param granter The granting subject
     * @param targetUserGroupId The target group
     * @return true if the subject is a super user or is in a parent group
     */
    public final boolean canGrantToGroup(final Subject granter, final String targetUserGroupId) {
        return granter.getIsSuperUser() || targetUserGroupId.startsWith(granter.getUserGroupId() + USER_GROUP_SEPARATOR);
    }
    
    /**
     * Grant the given permission to the target user group.
     *
     * @param userGroupId The user group that will be given the new permission.
     * @param permission The permission being granted.
     */
    protected abstract void grantPermission(final String userGroupId, final PERMISSION permission);
    
    /**
     * Revoke the given permission from the target user group.
     *
     * @param userGroupId The user group that will be given the new permission.
     * @param permission The permission being revoked.
     */
    protected abstract void revokePermission(final String userGroupId, final PERMISSION permission);
    
    /**
     * Check whether a user group has a given permission.
     *
     * @param userGroupId Identifier for the user group being checked.
     * @param permission The permission being checked.
     * @return True if the user group has the permission. Otherwise false.
     */
    protected abstract boolean hasPermission(final String userGroupId, final PERMISSION permission);
    
}

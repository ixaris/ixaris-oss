package com.ixaris.commons.microservices.defaults.context.authorisation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ixaris.commons.microservices.defaults.context.authorisation.DummyPermissionsService.Permission;

/**
 * Dummy implementation of {@link AbstractPermissionsService} used for unit testing purposes.
 *
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public class DummyPermissionsService extends AbstractPermissionsService<Permission> {
    
    public static final String PARENT_USER_GROUP = "A";
    public static final String CHILD_USER_GROUP = "A" + USER_GROUP_SEPARATOR + "A";
    public static final String OTHER_USER_GROUP = "B" + USER_GROUP_SEPARATOR + "B";
    
    private static final Map<String, Set<Permission>> RECOGNIED_PERMISSION = new HashMap<>();
    
    public enum Permission {
        P1,
        P2;
    }
    
    @Override
    protected synchronized void grantPermission(final String targetUserGroupId, final Permission permission) {
        RECOGNIED_PERMISSION.compute(targetUserGroupId, (k, v) -> {
            if (v == null) {
                return EnumSet.of(permission);
            }
            v.add(permission);
            return v;
        });
    }
    
    @Override
    protected synchronized void revokePermission(final String targetUserGroupId, final Permission permission) {
        RECOGNIED_PERMISSION.compute(targetUserGroupId, (k, v) -> {
            if (v != null) {
                if (v.remove(permission) && v.isEmpty()) {
                    return null;
                }
            }
            return v;
        });
    }
    
    @Override
    protected boolean hasPermission(final String userGroupId, final Permission permission) {
        return Optional.ofNullable(RECOGNIED_PERMISSION.get(userGroupId)).map(p -> p.contains(permission)).orElse(false);
    }
}

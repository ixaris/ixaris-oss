package com.ixaris.commons.microservices.defaults.context.authorisation;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;
import com.ixaris.commons.microservices.defaults.context.authorisation.DummyPermissionsService.Permission;

/**
 * Unit tests for the type {@link AbstractPermissionsService}.
 *
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public class AbstractPermissionsServiceTest {
    
    private DummyPermissionsService permissionsService = new DummyPermissionsService();
    
    @Test
    public void hasPermission_subjectIsSuperUser_shouldReturnTrue() {
        final Subject subject = Subject.newBuilder().setIsSuperUser(true).build();
        
        Assertions.assertThat(permissionsService.hasPermission(subject, Permission.P1)).isTrue();
    }
    
    @Test
    public void hasPermission_subjectIsNotASuperUser_shouldDelegateToConcreteImplementation_caseWhereDelegateReturnsTrue() {
        final Subject subject = Subject.newBuilder().setUserGroupId(DummyPermissionsService.CHILD_USER_GROUP).build();
        
        permissionsService.grantPermission(DummyPermissionsService.CHILD_USER_GROUP, Permission.P1);
        Assertions.assertThat(permissionsService.hasPermission(subject, Permission.P1)).isTrue();
        permissionsService.revokePermission(DummyPermissionsService.CHILD_USER_GROUP, Permission.P1);
        Assertions.assertThat(permissionsService.hasPermission(subject, Permission.P1)).isFalse();
    }
    
    @Test
    public void hasPermission_subjectIsNotASuperUser_shouldDelegateToConcreteImplementation_caseWhereDelegateReturnsFalse() {
        final Subject subject = Subject.newBuilder().setUserGroupId(DummyPermissionsService.OTHER_USER_GROUP).build();
        
        Assertions.assertThat(permissionsService.hasPermission(subject, Permission.P1)).isFalse();
    }
    
    @Test(expected = UnauthorisedException.class)
    public void grantPermission_subjectDoesNotHavePermission_shouldThrowUnauthorisedException() {
        permissionsService.grantPermission(Subject.getDefaultInstance(), DummyPermissionsService.OTHER_USER_GROUP, Permission.P1);
    }
    
    @Test
    public void grantPermission_subjectHasPermission_NoExceptionIsThrown() {
        try {
            final Subject superUser = Subject.newBuilder().setIsSuperUser(true).build();
            permissionsService.grantPermission(superUser, DummyPermissionsService.PARENT_USER_GROUP, Permission.P1);
            final Subject subject = Subject.newBuilder().setUserGroupId(DummyPermissionsService.PARENT_USER_GROUP).build();
            permissionsService.grantPermission(subject, DummyPermissionsService.CHILD_USER_GROUP, Permission.P1);
        } catch (final RuntimeException exception) {
            Assertions.fail(String.format("Expected no exception to be thrown, but caught an exception of type [%s] with message [%s]",
                exception.getClass().getName(),
                exception.getMessage()));
        } finally {
            permissionsService.revokePermission(DummyPermissionsService.CHILD_USER_GROUP, Permission.P1);
            permissionsService.revokePermission(DummyPermissionsService.PARENT_USER_GROUP, Permission.P1);
        }
    }
    
}

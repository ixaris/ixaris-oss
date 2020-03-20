# Default Context

The default context defined the following:

## Subject

The subject contains information on the subject doing an operation.

### Authorisation

Authorisation can be done based on the subject's `user_group_id`. By default, permissions are associated to 
a user group, which is linked to credentials. To check if a subject is authorised, the user group is checked
to determine whether the required permission is granted.

For managing user groups, a subject's authorisation to grant a permission is checked. By default, the superuser 
is allowed to grant to anyone, otherwise the targetGroupId must be prefixed with the granter's userGroupId 
and separator

E.g. given `_` as a separator, granter in group `group1` can grant to group `group1_1` and `group1_2` but not 
to `group10` or `group50`


## Resume

Idempotent processes may be resumed. Some processes, however, will require some additional information to be resumed, 
typically when interacting with a non-idempotent service that requires manual intervention to resume. The context allows 
the propagation of an opaque resume info object and an associated type for this purpose. 
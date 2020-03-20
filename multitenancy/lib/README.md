# Multitenancy

This library hides multiple instances, one per tenant, behind a multi-tenanted instance. This is useful for 
abstracting handling of multi-tenancy out of most code. The active tenant can be assigned or queries using the 
`MultiTenancy.TENANT` async local. 

## Lifecycle Participants

When a tenant is added or removed, `TenantLifecycleParticipant`s react by potentially creating or destroying
instances for that tenant. A tenant is considered active when all participants report successful activation. 
At this point the tenant active event is published to registered `TenantLifecycleListeners`

Getting configuration for the activated instances is the responsibility of the participant, and this library 
makes no attempt to provide such configuration. Examples of such configuration are connection strings and 
credentials for data sources.

```plantuml
digraph G {
    INACTIVE -> PRE_ACTIVATING
    PRE_ACTIVATING -> ACTIVATING
    ACTIVATING -> ACTIVE
    ACTIVE -> DEACTIVATING
    ACTIVATING -> DEACTIVATING
    DEACTIVATING -> POST_DEACTIVATING
    PRE_ACTIVATING -> POST_DEACTIVATING
    POST_DEACTIVATING -> INACTIVE
}
```
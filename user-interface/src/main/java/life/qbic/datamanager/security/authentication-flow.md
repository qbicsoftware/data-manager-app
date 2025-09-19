## Current flows of Prinpicals through the application

```mermaid
flowchart
    loginWithPassword --> QbicUserDetails
    loginWithOrcid -- already registered with oidc --> QbicOidcUser
    loginWithOrcid -- no known user with oidc --> DefaultOidcUser
    QbicUserDetails -- link oidc to account --> DefaultOidcUserFromOrcid -- add information to QbicUserDetails --> QbicUserDetails
    DefaultOidcUser -- register account --> QbicOidcUser
```

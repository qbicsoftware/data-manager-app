# Service API

The idea of the service API is to create a uniform interface to interact with Data Manager ressources from a user agent, for example the browser. Since the project uses Vaadin, the API is written in Java and provides an easy integration
in Vaadin Flow.

The API provides access and manipulation of resources similar to what you know from REST, with slightly different names though:

- Create (similar to POST)
- Get (similar to GET)
- Update (similar to PUT, partial updates are possible)
- Delete (similar to DELETE)

## Create example
```mermaid
---
title: Create Example
---
classDiagram
    class AsyncProjectService {
        Mono~ProjectCreationResponse~ update(ProjectCreationResponse request)
    }
    <<interface>> AsyncProjectService
    AsyncProjectService ..> ProjectCreationRequest
    AsyncProjectService ..> ProjectCreationResponse
    
    class ProjectCreationRequest {
        String projectId
        ProjectCreationRequestBody requestBody
        String requestId
    }
    
    class ProjectCreationResponse {
        String projectId
        ProjectCreationResponseBody responseBody
        String requestId
    }
    
    class ProjectCreationRequestBody {
       
    }
    <<interface>> ProjectCreationRequestBody
    
    class ProjectCreationResponseBody {
        
    }
    <<interface>> ProjectCreationResponseBody
    
    class ProjectInformation
    
    ProjectInformation ..|> ProjectCreationRequestBody
    ProjectInformation ..|> ProjectCreationResponseBody
  
    ProjectCreationRequest ..> ProjectCreationRequestBody
    ProjectCreationResponse ..> ProjectCreationResponseBody
    
    
    
```

## Get example

## Update example
```mermaid
---
title: Update Example
---
classDiagram
    class AsyncProjectService {
        Mono~ProjectUpdateResponse~ update(ProjectUpdateRequest request)
    }
    <<interface>> AsyncProjectService
    AsyncProjectService ..> ProjectUpdateRequest
    AsyncProjectService ..> ProjectUpdateResponse
    
    class ProjectUpdateRequest {
        String projectId
        ProjectUpdateRequestBody requestBody
        String requestId
    }
    
    class ProjectUpdateResponse {
        String projectId
        ProjectUpdateResponseBody responseBody
        String requestId
    }
    
    class ProjectUpdateRequestBody {
       
    }
    <<interface>> ProjectUpdateRequestBody
    
    class ProjectUpdateResponseBody {
        
    }
    <<interface>> ProjectUpdateResponseBody
    
    
    class FundingInformation 
    FundingInformation ..|> ProjectUpdateRequestBody
    FundingInformation ..|> ProjectUpdateResponseBody
  
    ProjectUpdateRequest ..> ProjectUpdateRequestBody
    ProjectUpdateResponse ..> ProjectUpdateResponseBody
    
    
    
```

## Delete Example

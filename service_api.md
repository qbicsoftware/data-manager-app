# Service API

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

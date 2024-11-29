# Frontend components 
Some visual aid of our custom view components structure.

## Dialog window

```mermaid
---
title: Dialog window
---

classDiagram
    note for Component "Vaadin Component"
    note for Dialog "Vaadin Component"
    AppDialog <-- DialogHeader
    AppDialog <-- DialogBody
    AppDialog <-- DialogFooter
    DialogBody ..|> UserInput
    UserInput --> InputValidation
    AppDialog --> Component
    AppDialog --> Action
    AppDialog --|> Dialog
    AppDialog --> UserInput
    DialogBody *-- DialogSection
    
    class Dialog {
        
    }
    
    class DialogSection {
        
    }

    class Component {
    }

    class AppDialog {
        +setHeader(Component component)
        +setBody(Component component)
        +setFooter(Component component)
        +registerConfirmAction(Action action)
        +registerCancelAction(Action action)
        +registerUserInput(UserInput input)
        
        +confirm()
        +cancel()
    }

    class DialogHeader {
    }

    class DialogBody {
    }

    class Action {
        <<interface>>
        +execute()
    }

    class DialogFooter {
    }

    class InputValidation {
        + passed() boolean
        + failed() boolean
    }

    class UserInput {
        <<interface>>
        + validate() InputValidation
        + hasChanges() boolean
    }

```

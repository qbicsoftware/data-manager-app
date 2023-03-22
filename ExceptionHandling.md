# Exception handling

## Common Anti-Pattern

### Diaper (swallowing exceptions)

```java
try{
        doSomeWork();
        }catch(Exception e){
        }
```

What happened here? The catch block is empty. This should never happen as it makes debugging
impossible. An empty catch block is a massive red flag.

```java
try{
        doSomeWork();
        }catch(Exception e){
        e.printStackTrace();
        }
```

This should be fine, right? No. The `printStackTrace()` method prints to `System.err`. This will not
suffice as we cannot be confident that `System.err` is captured in the log files. The exception
might not be detected or available for debugging purposes.

### Decapitation

If we are not encouraged to print the stack trace to `System.err`, then we can throw
a `RuntimeException`.
The following code should solve our problem, right?

```java
try{
        doSomeWork();
        }catch(Exception e){
        throw new RuntimeException("I assume an exception due to XYZ")
        }
```

Sadly no. By not setting the cause of the new exception to the caught exception, we loose all
information about why the exception occurred.
We cannot understand what happened from the rethrown exception's stack trace. Thus, always set the
cause of thrown exceptions.

### Checked Exceptions

Checked exceptions in Java have to be handled by the calling code. This presents an abstraction
leak.
Why should the calling code know details about the implementation?

```java
public static int countUserPosts()throws IOException{
        Properties userProps=new Properties();
        try(FileReader reader=new FileReader("my-users.properties")){
        userProps.load(reader);
        }
        return userProps.getProperty("user.posts.count");
        }
```

The code above forces the calling code of `countUserPosts()` to handle `IOException`.
How should the caller decide what to do? It does not even know that some property file is read,
this is an implementation detail of the `countUserPosts()` method.

Instead consider an approach like the following:

```java
public static int countUserPosts(){
        try{
        Properties userProps=new Properties();
        try(FileReader reader=new FileReader("my-users.properties")){
        userProps.load(reader);
        }
        return userProps.getProperty("user.posts.count");
        }catch(IOException e){
        throw new RuntimeException(e);
        }
        }
```

### Exceptions as control flow

Exceptions are slow. It is cheaper to validate first and then proceed than to throw an exception and
handle it.
When using exceptions for control flow, programmers are forced to write catch blocks and the chance
for swallowing an exception or decapitating an exception increases.

Thus, avoid exceptions for expected or likely outcomes.

```java
public static void isValidProjectCode(String code){
        try{
        new ProjectCode(code,MY_CONSTANT);
        }catch(EmptyProjectCodeException|IllegalArgumentException|ToLongProjectCodeException e){
        return false;
        }
        return true;
        }
```

becomes

```java
public static void isValidProjectCode(String code){
        return ProjectCode.validate(code,MY_CONSTANT);
        }
```

When using custom exceptions as flow control, make them extend `Exception` and
not `RuntimeException`
because you intend the calling code to use them to determine the application flow.
As previously discussed, all problems with checked exceptions [discussed here](#checked-exceptions)
apply.
I think this illustrates easily why exceptions for control flow are a **bad idea**.

If, on the other hand you reasonably expect a value to be present when no value is present, an
exceptional state would follow.
Thus throwing exceptions is fine in that case. For example:

```java
public static void soSomeStuffWithMyProjectCode(String code){ // code is always expected to be valid
        var myProjectCode=new ProjectCode(code,MY_CONSTANT);
        ...(some complex logic involving myProjectCode)
        }

public ProjectCode(String code,Constant constant){
        if(!validate(code,constant)){
        throw new RuntimeException("We encounter something unexpected.");
        }
        }
```

In those cases we would not `catch` the exception and try
to [handle it as late as possible](#global-exception-handler).

## Good practices

### Global Exception Handler

### Throw early, handle late

### Catch-Rethrow

1. user-friendly message
2. unit test a thrown exception
3. report debug information in new message
4. get rid of checked exception

https://blog.awesomesoftwareengineer.com/p/throwing-exceptions-vs-control-flow

Throw before mutating an object state: leave the objects’ state unaltered
Don’t use exceptions for normal flow control: they are expensive, and they can smell like a GOTO
instruction
Your developers should feel safe that exceptions are never lost.

## Do I use `life.qbic.application.commons.Result` or throw an exception?

## Vaadin, which exceptions to use for routing?

## When to rethrow an exception?

* don't do checked exceptions
* never catch an exception and do nothing // diaper anti-pattern
* don't catch Exception
* throw runtime exceptions wrapping checked exceptions

- log and rethrow wrapped -> logged multiple times? What actually happened? // log-rethrow
  anti-pattern
- global exception handler
- what to show the user?
  - NEVER the stacktrace!
  - don't show the exception message -> mixing presentation with detecting the business condition
    for the exception to occur.
  - int -> error code manual :(
  - enum -> with message to the user
- only catch recoverable exceptions
- for recoverable exceptions -> subtype application exception
- distinguish between exceptions by enum reason
- include interesting values for debugging to the exception message
- DO NOT DECAPITATE exception!

Reasons to catch-rethrow

1. user-friendly message
2. tests
3. developers (adding debug information)
4. get rid of checked exception

Anit-Patterns:

* Diaper
* Decapitation
* Log-Rethrow

NO SHAWARMA STYLE EXCEPTION-HANDLING <- Diaper

how we do it in vaadin:
exceptions thrown end up in the view as notifications.
exceptions thrown during navigation end up in own view

What does `fail` mean?
Do we need custom exceptions? If so, how to handle?
Force the consumer of the result to handle different paths?
Force the supplier of the result to supply different paths?

scenarios:

1. I want to query for some information from the backend: did it work? (maybe result object)
2. I am calling other backend logic
3. I am calling some method I expect to fail

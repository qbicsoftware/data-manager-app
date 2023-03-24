# Exception handling

## Common Anti-Pattern

### Diaper (swallowing exceptions)

```groovy
try {
   doSomeWork();
} catch (Exception e) {
}
```

What happened here? The catch block is empty. This should never happen as it makes debugging
impossible. An empty catch block is a massive red flag.

```groovy
try {
   doSomeWork();
} catch (Exception e) {
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

```groovy
try {
   doSomeWork();
} catch (Exception e) {
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

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static int countUserPosts() throws IOException {
   Properties userProps = new Properties();
   try (FileReader reader = new FileReader("my-users.properties")) {
      userProps.load(reader);
   }
   return userProps.getProperty("user.posts.count");
}
```

The code above forces the calling code of `countUserPosts()` to handle `IOException`.
How should the caller decide what to do? It does not even know that some property file is read,
this is an implementation detail of the `countUserPosts()` method.

Instead consider an approach like the following:

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static int countUserPosts() {
   try {
      Properties userProps = new Properties();
      try (FileReader reader = new FileReader("my-users.properties")) {
         userProps.load(reader);
      }
      return userProps.getProperty("user.posts.count");
   } catch (IOException e) {
      throw new RuntimeException(e);
   }
}
```

### Log-Rethrow Anti-Pattern

When we catch an exception surely we want to be totally sure we have logged it, don't we? So let's
just catch any exception and make sure

```groovy
public static void someMethod() {
   try {
      // ...
      doSomeStuff(); // e.message = Oh no! Another exception.
      // ...
   } catch (ApplicationException e) {
      log.error(e);
      throw new ApplicationException(e);
   }
}
```

Perfect. Now we have the exception in the log 💪😃!

...

But wait ...

When we inspected the log we observed "Oh no! Another exception." several times. Why, when did it
occur?
The confusion inceases...

Later, after hours of debugging, we notice the following code:

```groovy
public static void anotherMethod() {
   try {
      // ...
      someMethod();
      // ...
   } catch (ApplicationException e) { // the re-thrown exception
      log.error(e);
      throw e;
   }
}
```

We caught, logged and threw the same exception over again. Wow, so all the time we only had one
exception and not several?

Thanks to the redundant catching and logging of the same exception within the called method and after the method, reading the log file was more confusing than it had to be
We would have ended up with only one exception in our log file, stating where it occurred and that
it occured only once.
We would have saved many hours of digging through the code.
**Never** only catch an exception to **log and rethrow** it.
Only [catch exception when necessary](#catch-rethrow).

### Exceptions as control flow

Exceptions are slow. It is cheaper to validate first and then proceed than to throw an exception and
handle it.
When using exceptions for control flow, programmers are forced to write catch blocks and the chance
for swallowing an exception or decapitating an exception increases.

Thus, avoid exceptions for expected or likely outcomes. For expected outcomes consider returning a
meaningful result instead. (_Hint: Java Records present an easy way to implement result objects_)

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static boolean isValidProjectCode(String code) {
   try {
      new ProjectCode(code, "some value");
   } catch (NullPointerException | IllegalArgumentException e) {
      return false;
   }
   return true;
}
```

becomes

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static boolean isValidProjectCode(String code) {
   return ProjectCode.validate(code, "some value");
}
```

When using custom exceptions as flow control, make them extend `Exception` and
not `RuntimeException`
because you intend the calling code to use them to determine the application flow.
As previously discussed, all problems with checked exceptions [discussed here](#checked-exceptions)
apply.
I think this illustrates easily why exceptions for control flow can be a bad idea.

If, on the other hand you reasonably expect a value to be present when no value is present, an
exceptional state would follow.
Thus throwing exceptions is fine in that case. For example:

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static void soSomeStuffWithMyProjectCode(String code) {
   // code is always expected to be valid
   var myProjectCode = new ProjectCode(code, "some value");
   // ... (some complex logic involving myProjectCode)
}

public ProjectCode(String code, Constant constant) {
   if (!validate(code, constant)) {
      throw new RuntimeException("We encountered something unexpected.");
   }
}
```

In those cases we would not `catch` the exception and try
to [handle it as late as possible](#throw-early-handle-late).

**Exceptions as control-flow smell like a _GOTO_ instruction!**
![](https://imgs.xkcd.com/comics/goto.png)

## Good practices

### Throw early, handle late

When encountering exceptional circumstances, additional work should be avoided. Imagine an
application calculating salaries for people working at a firm.
One employee, Alice, left the company recently. Let's assume that the computation of a salary is
complex.

If the application would first calculate the salary for Alice and then try to retrieve her bank
account from a list of employees,
an exceptional situation would occur. _(Yes, one could argue that people leaving a company is not
that uncommon but let's assume it is)_.

Our application would fail after doing all the heavy lifting computing the salary. This is bad. A
lot of resources wasted.

Instead we would want the application to fail as early as possible. Thus we check for Alice's bank
account first and throw there if the expected information is not present.

_**Try to throw before mutating an object state. Leave the objects’ state unaltered.**_

### When to catch exceptions

Generally, you should avoid catching exceptions where you cannot act on them.
The diaper and the log-rethrow anti-pattern indicate a caught exception in the wrong place.
There are some cases in which you want to catch exceptions.

#### Catch and Resolve

If you can resolve the exceptional state and return back to a normal state, you can catch the
exception.
However, be careful that you consider this decision wisely as it poses the question: If the
exception can be resolved, is it really exceptional?
If the answer is no, you should revisit your code and consider using a meaningful return value or
whether to split your method with SRP instead of throwing an exception.

#### Catch-Rethrow

In some cases it can be useful to catch an exception, do something with it and rethrow that
exception.

1. **user-friendly message**: In case you want to wrap your exception in another exception with
   information necessary to provide the user with a friendly and understandable message.
2. **unit test a thrown exception**: In case you write a unit test that checks for an exception
   being thrown. Use with care.
3. **report debug information in new message**: Wrapping the exception in another exception with
   useful debug information in the message can make debugging easier as the information will end up
   in the logs.
4. **get rid of checked exception**: As [discussed earlier](#checked-exceptions), we don't want the
   abstraction leak of checked exceptions.

#### Global exception handling

So we never want to catch unwanted exceptions. How do we save our users from bombardment with stack
traces?
We learned that we should **never expose stack traces to users** as this is a security issue.
So should I rather swallow exceptions and ignore
the [diaper anti-pattern](#diaper-swallowing-exceptions)?

As a developer, you want to feel safe and rest assured that all exceptions are handled somewhere.

The solution is to create a safety net, catching all exceptions that occur. A global exception
handler!
Developers can throw exceptions whenever they want without needing to ensure that they are handled
and without being afraid to expose details to users of your software.

A global exception handler should sit between your software and the
user. [In Spring applications this can be at the REST API level](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html).
As Vaadin computes the view server-side, we can use its exception and error handling classes as
global exception handlers;
See [Routing Exception Handler in Vaadin](https://vaadin.com/docs/v23/routing/exceptions)
and [Custom Exception Handler in Vaadin](https://vaadin.com/docs/v23/routing/exceptions#custom-exception-handlers).


[//]: # (## FAQ)

[//]: # ()

[//]: # (**DO NOT USE our Result type as return value.... the caller can ignore it and the exceptions are lost**)

[//]: # (https://blog.softwaremill.com/exceptions-no-just-try-them-off-497984eb470d)

[//]: # ()

[//]: # ()

[//]: # (https://blog.awesomesoftwareengineer.com/p/throwing-exceptions-vs-control-flow)

[//]: # ()

[//]: # (Throw before mutating an object state: leave the objects’ state unaltered)

[//]: # (Don’t use exceptions for normal flow control: they are expensive, and they can smell like a GOTO)

[//]: # (instruction)

[//]: # (Your developers should feel safe that exceptions are never lost.)

[//]: # (## Do I use `life.qbic.application.commons.Result` or throw an exception?)

[//]: # ()

[//]: # (## Vaadin, which exceptions to use for routing?)

[//]: # ()

[//]: # (## When to rethrow an exception?)

[//]: # ()

[//]: # (* don't do checked exceptions)

[//]: # (* never catch an exception and do nothing // diaper anti-pattern)

[//]: # (* don't catch Exception)

[//]: # (* throw runtime exceptions wrapping checked exceptions)

[//]: # ()

[//]: # (- log and rethrow wrapped -> logged multiple times? What actually happened? // log-rethrow)

[//]: # (  anti-pattern)

[//]: # (- global exception handler)

[//]: # (- what to show the user?)

[//]: # (    - NEVER the stacktrace!)

[//]: # (    - don't show the exception message -> mixing presentation with detecting the business condition)

[//]: # (      for the exception to occur.)

[//]: # (    - int -> error code manual :&#40;)

[//]: # (    - enum -> with message to the user)

[//]: # (- only catch recoverable exceptions)

[//]: # (- for recoverable exceptions -> subtype application exception)

[//]: # (- distinguish between exceptions by enum reason)

[//]: # (- include interesting values for debugging to the exception message)

[//]: # (- DO NOT DECAPITATE exception!)

[//]: # ()

[//]: # (how we do it in vaadin:)

[//]: # (exceptions thrown end up in the view as notifications.)

[//]: # (exceptions thrown during navigation end up in own view)

[//]: # ()

[//]: # (What does `fail` mean?)

[//]: # (Do we need custom exceptions? If so, how to handle?)

[//]: # (Force the consumer of the result to handle different paths?)

[//]: # (Force the supplier of the result to supply different paths?)

[//]: # ()

[//]: # (scenarios:)

[//]: # ()

[//]: # (1. I want to query for some information from the backend: did it work? &#40;maybe result object&#41;)

[//]: # (2. I am calling other backend logic)

[//]: # (3. I am calling some method I expect to fail)

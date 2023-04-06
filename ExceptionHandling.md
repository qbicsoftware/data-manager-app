# Exception handling

## Common Anti-Pattern

### Diaper (swallowing exceptions)

* Makes the needle disappear from the haystack.
* Debugging is impossible (or very hard at least)

```groovy
try {
    doSomeWork();
} catch (Exception e) {
}
```

```groovy
try {
   doSomeWork();
} catch (Exception e) {
   e.printStackTrace();
}
```

The `printStackTrace()` method prints to `System.err`. Often, **`System.err` is not captured by the
logs.**

### Decapitation

Decapitated exceptions provide less information about the systems state producing the exception.
Never do it.

```groovy
try {
   doSomeWork();
} catch (Exception e) {
    throw new RuntimeException("my crazy method threw an exception")
}
```

#### Instead do:

```groovy
try {
    doSomeWork();
} catch (Exception e) {
    throw new RuntimeException("my crazy method threw an exception", e)
}
```

### Checked Exceptions

* Forces caller to catch the exception.
* Caller might have no way to handle it
* Presents an abstraction leak as the caller needs to deal with internals of the implementation.

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

#### Instead do:
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

* Do not only log an exception and rethrow
* Pollutes the log
* Does not add anything but confusion

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

When we catch an exception surely we want to be totally sure we have logged it, don't we? So let's
just catch any exception and make sure.

Perfect. Now we have the exception in the log 💪😃!

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

Thanks to the redundant catching and logging of the same exception within the called method and
after the method, reading the log file was more confusing than it had to be
We would have ended up with only one exception in our log file, stating where it occurred and that
it occured only once.
We would have saved many hours of digging through the code.
**Never** only catch an exception to **log and rethrow** it.

#### Instead do: [catch exception when necessary](#catch-rethrow).

### Exceptions as control flow

**Exceptions as control-flow smell like a _GOTO_ instruction!**<br>
They do not have any scope and can run wild in your code!

![](https://imgs.xkcd.com/comics/goto.png)

* Exceptions are expensive
* Exceptions indicate **exceptional** system state
* catch blocks -> possibilities for _diaper_, _decapitation_, _GOTOs_

Exceptions are slow. It is cheaper to validate first and then proceed than to throw an exception and
handle it.
When using exceptions for control flow, programmers are forced to write catch blocks and the chance
for swallowing an exception or decapitating an exception increases. Developers might forget to catch
an exception or if the exception was added later, legacy code might not be aware of the possibility
and miss some previous type of exception.

Thus, avoid exceptions for expected or likely outcomes. For expected outcomes consider returning a
meaningful result instead. (_Hint: Java Records present an easy way to implement result objects_)

<!-- we use groogy as languate so the formatting works ;D -->

```groovy
public static void isShorterThan(String input, int maxLength) {
    if (input.length() >= maxLength) {
        throw new LengthException("The string is to long");
    }
}

public static void isLongerThan(String input, int minLength) {
    if (input.length() <= minLength) {
        throw new LengthException("The string is to short");
    }
}

public static boolean isZipCode(String code) {
    try {
        isLongerThan(code, 4);
        isShorterThan(code, 6);
        return true;
    } catch (LengthException e) {
        return false;
    }
}
```

Replacing exceptions as control flow:

```groovy
public static boolean isShorterThan(String input, int maxLength) {
    return input.length() >= maxLength;
}

public static boolean isLongerThan(String input, int minLength) {
    return input.length() <= minLength;
}

public static boolean isZipCode(String code) {
    return isLongerThan(code, 4) && isShorterThan(code, 6);
}
```

If you **reasonably expect** a value to be present when no value is present, or a variable to have a
specific value, throwing exceptions is fine. For example:

```groovy
public static void doSomeObjectStuff(Object object) {
    if (object == null) {
        throw new NullPointerException("object is null");
    }
    // ... some code working with object
}

public static void doStuff() {
    Object myObject = createObject();
    doSomeObjectStuff(myObject);
}
```

Here we assume that we are given an object in the `doSomeObjectStuff` method. If this is not the
case, something is off and we panic.

**Note:** In those cases we do not `catch` the exception and try
to [handle it as late as possible](#throw-early-handle-late).

## Good practices

### Throw early, handle late

When encountering exceptional circumstances, additional work should be avoided.
We want the application to fail as early as possible.

_**Throw before mutating state. Leave the state unaltered.**_

### When to catch exceptions

Avoid catching exceptions where you cannot act on them.

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

It can be useful to catch an exception, do something with it and rethrow that
exception. Only catch-rethrow if you

- **user-friendly message**:
    - wrap your exception to provide the user with a friendly and understandable message.
- **unit test a thrown exception**:
    - write a unit test that checks for a thrown exception.
- **report debug information in new message**:
    - wrap the exception in another exception with useful debug information in the message.
- **get rid of checked exception**:
    - [discussed earlier](#checked-exceptions)

#### Global exception handling

So we never want to catch unwanted exceptions. How do we save our users from bombardment with stack
traces?

We learned that we should **never expose stack traces to users** as this is a security issue.
So should I rather swallow exceptions and ignore
the [diaper anti-pattern](#diaper-swallowing-exceptions)?

As a developer, you want to **feel safe and rest assured that we can throw exceptions any time and
anywhere**.

The solution is to create a **application global safety net**, catching all exceptions that occur. A
global exception
handler!
Developers can throw exceptions whenever they want without needing to ensure that they are handled
and without being afraid to expose details to users of your software.

A global exception handler should sit between your software and the
user. [In Spring applications this can be at the REST API level](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html).
As Vaadin computes the view server-side, we can use its exception and error handling classes as
global exception handlers;
See [Routing Exception Handler in Vaadin](https://vaadin.com/docs/v23/routing/exceptions)
and [Custom Exception Handler in Vaadin](https://vaadin.com/docs/v23/routing/exceptions#custom-exception-handlers).

## Scenarios

##### I want to display an error message at the point where the user entered information.

There are a couple of problems you need to address:

1. How can I identify the input location at the place where the error is detected?
2. How expected is the invalid input? How frequently does it occur?
    - If it is expected: Why did I decide against an explicit response object?
    - If it is unexpected, how can I show a specific error message?
3. What is the closest point where I can show the error message?
4. Catch or do a `Result.of` call?

##### I want to re-direct to a specific error page for certain errors.

When does re-direction make sense? Is it only when navigating, that redirecting to a certain error
page makes sense, or are the other cases?
A different handler can take care of exceptions during navigation. (Does it make sense to subclass
the main exception?)

##### I want to display an error message at a specific location no matter where the exception occurred.

At the owner of that location catch and modify ui.

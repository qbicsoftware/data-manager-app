<div align="center">

# Data Manager

Data Manager - A web-based multi-omics data management platform enabling sustainable long-term
scientific FAIR-compliant data access.

[![Build Maven Package](https://github.com/qbicsoftware/data-manager-app/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/data-manager-app/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/data-manager-app/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/data-manager-app/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/data-manager-app/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/data-manager-app/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/data-manager-app?include_prereleases)](https://github.com/qbicsoftware/data-manager-app/releases)

[![license](https://img.shields.io/github/license/qbicsoftware/data-manager-app)](https://github.com/qbicsoftware/data-manager-app/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-groovy,%20java-blue.svg)

</div>

## Overview:

- [How to Run](#how-to-run)
    * [Configuration](#configuration)
        * [Java Version](#java-version)
        * [Environment Variables](#environment-variables)
        * [Properties](#properties)
    * [Local Testing](#local-testing)
    * [Production Deployment](#production-deployment)
- [How to Use](#how-to-use)
    * [User Login](#user-login)
    * [User Registration](#user-registration)
- [Application Overview](#application-overview)
    * [Project Structure](#project-structure)
    * [Vaadin Framework](#vaadin-framework)
        * [Additional Information](#additional-information)
- [License](#license)

## How to Run

This application is based on maven and can be run after setting the
required [configurations](#configuration),
by typing `mvnw` (Windows), or `./mvnw` (Mac & Linux) in the command line and opening
http://localhost:8080 in your browser.

You can also import the project to your IDE of choice as you would with any
Maven project. Read more on [how to import Vaadin projects to different
IDEs](https://vaadin.com/docs/latest/flow/guide/step-by-step/importing) (Eclipse, IntelliJ IDEA,
NetBeans, and VS Code).

### Configuration

#### Java Version

This application requires [Java 17](https://openjdk.java.net/projects/jdk/17/) to be build and run

#### Environment Variables

The environment variables specify the information for the connection to the mail server and user
database, both of which are necessary for this application to run.

| Environment Variable | Description               | Default Value                           |                
|----------------------|---------------------------|-----------------------------------------|
| `USER_DB_URL`        | The database host address | driver:database://123.4.56.789:8080     |
| `USER_DB_USER_NAME`  | The database user name    | myusername                              |
| `USER_DB_USER_PW`    | The database password     | astrongpassphrase                       |
| `PORT`               | The application port      | 8080                                    |
| `USER_DB_DRIVER`     | The database driver       | com.mysql.cj.jdbc.Driver                |
| `MAIL_HOST`          | The smtp server host      | smtp.gmail.com                          |
| `MAIL_PASSWORD`      | The password to authenticate against the mail server | astrongemailpassphrase                  |
| `Mail_USERNAME`      | The username to authenticate against the mail server | myemailusername                         |

#### Properties

The environment variables can either be set in the runtime configuration of your IDE or directly in
the [application properties file](https://github.com/qbicsoftware/data-manager-app/blob/main/webapp/src/main/resources/application.properties):

```properties
spring.datasource.url=${USER_DB_URL:localhost}
spring.datasource.username=${USER_DB_USER_NAME:myusername}
spring.datasource.password=${USER_DB_USER_PW:astrongpassphrase!}
server.port=${PORT:8080}
spring.datasource.driver-class-name=${USER_DB_DRIVER:com.mysql.cj.jdbc.Driver}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
```

### Local testing

The default configuration of the app binds to the local port 8080 to the systems localhost. \
After starting the application it will be accessible at http://localhost:8080 in a browser of your
choice.

```
http://localhost:8080
```

### Production Deployment

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux). \
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed.
The file can be found in the `target` folder after the build completes:

    |-target
    |---datamanager-{version}.jar
    |---...

Once the JAR file is built, you can run it using
`java -jar target/datamanager-{version}.jar`

## How To Use

This guide intends to showcase the features of the data-manager-application

### User Login

After startup the data manager application will redirect the user to the login screen hosted by
default at

```
http://localhost:8080/login
```

This view enables the user to login into an already existing account by providing the required
credentials.

![add](docs/readme/LoginScreen.png)

Additionally, in this screen the user can request a password reset for his account if necessary. \
The user will then be contacted via the provided emailAddress address with the steps necessary to perform a
password reset.\
Finally, the user is able to switch to the [registration screen](#user-registration) by clicking on
the register button or the registration link

### User Registration

This view is accessible by clicking on the register button or the registration link in
the [Login Screen](#user-login).
It is hosted by default at:

```
http://localhost:8080/register
```

This view enables the user to register a new account by providing the required credentials:

![add](docs/readme/RegistrationScreen.png)

After successful registration the user will be contacted via the provided emailAddress address with the
steps necessary to authenticate the generated account.

## Application overview

### Project structure

The project is composed of a [multi-module maven](https://maven.apache.org/) structure divided into
a `domain` and `webapp` module.
The `domain` module hosts the business logic for user and data management.

Examples include:

- `UserRegistrationService.java` in `src/main/java/life/qbic/apps/datamanager/services/` contains
  the application service used to register users for the user management domain context.
- `policies` package in `src/main/java/domain/usermanagement/` contains the business logic to
  validate provided user information.
- `repository` folder in `src/main/java/domain/usermanagement/` contains the connection logic
  between the application and the user database.

In contrast, the `webapp` module hosts the frontend components and services provided in the
application.

Examples include:

- `MainLayout.java` in `src/main/java/views/` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/components/vaadin-app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.

### Vaadin Framework

This application employs the frontend components released in version 23 of
the [vaadin framework](https://github.com/vaadin)

#### Additional Information

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified
  at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples
  at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities
  at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Discover Vaadin's set of CSS utility classes that enable building any UI without custom CSS in
  the [docs](https://vaadin.com/docs/latest/ds/foundation/utility-classes).
- Find a collection of solutions to common use cases
  in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join
  the vaadin [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in the vaadin [GitHub](https://github.com/vaadin/platform).

## License

This work is licensed under the [MIT license](https://mit-license.org/).

This work uses the [Vaadin Framework](https://github.com/vaadin), which is licensed
under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).

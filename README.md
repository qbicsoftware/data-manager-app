# Data Manager

This project can be used as a starting point to create your own Vaadin application with Spring Boot.
It contains all the necessary configuration and some placeholder files to get you started.

## Running the application

The project is a standard Maven project. To run it from the command line,
type `mvnw` (Windows), or `./mvnw` (Mac & Linux), then open
http://localhost:8080 in your browser.

You can also import the project to your IDE of choice as you would with any
Maven project. Read more on [how to import Vaadin projects to different 
IDEs](https://vaadin.com/docs/latest/flow/guide/step-by-step/importing) (Eclipse, IntelliJ IDEA, NetBeans, and VS Code).

## Deploying to Production

To create a production build, call `mvnw clean package -Pproduction` (Windows),
or `./mvnw clean package -Pproduction` (Mac & Linux).
This will build a JAR file with all the dependencies and front-end resources,
ready to be deployed. The file can be found in the `target` folder after the build completes.

Once the JAR file is built, you can run it using
`java -jar target/datamanager-1.0-SNAPSHOT.jar`


### Configuration

#### Properties

The default configuration of the app binds to the local port 8080 to the systems localhost:

```
http://localhost:8080
```

#### Environment Variables
The env variables contain information about the salt and the secret. Both of them are used to encrypt and decrypt user information.

| environment variable       | description               |
|----------------------------|---------------------------|
| `USER_DB_URL`              | The database host address |
| `USER_DB_USER_NAME`        | The database user name    |
| `USER_DB_USER_PW`          | The database password     |

The application properties file could look like the following:
```properties
spring.datasource.url=${USER_DB_URL:localhost}
spring.datasource.username=${USER_DB_USER_NAME:myusername}
spring.datasource.password=${USER_DB_USER_PW:astrongpassphrase!}
```
To change the port or the driver those can be added as environmental variables as well. Both are preset with default values and
are therefore not mandatory to set

| environment variable | description          |
|----------------------|----------------------|
| `PORT`               | The application port |
| `USER_DB_DRIVER`     | The database driver  |

```properties
server.port=${PORT:8080}
spring.datasource.driver-class-name=${USER_DB_DRIVER:com.mysql.cj.jdbc.Driver}
```

## Project structure

- `MainLayout.java` in `src/main/java` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/components/vaadin-app-layout).
- `views` package in `src/main/java` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorials at [vaadin.com/tutorials](https://vaadin.com/tutorials).
- Watch training videos and get certified at [vaadin.com/learn/training](https://vaadin.com/learn/training).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/components](https://vaadin.com/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Discover Vaadin's set of CSS utility classes that enable building any UI without custom CSS in the [docs](https://vaadin.com/docs/latest/ds/foundation/utility-classes). 
- Find a collection of solutions to common use cases in [Vaadin Cookbook](https://cookbook.vaadin.com/).
- Find Add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin/platform).

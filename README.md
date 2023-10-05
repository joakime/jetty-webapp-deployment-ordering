# WebApp Deployment Ordering Demo

This example project exists to demonstrate how to control webapp deployment ordering.

What you'll need for this project.

* OpenJDK 11 (or newer)
* Maven 3.9.2 (or newer)

## Prepare Project

Use the command line ...

``` shell
mvn clean install 
```

## Tour of the Project

We have 2 webapps, called `webapp-A` and `webapp-B`.

The webapp `webapp-A/` :
 * has an index.html
 * has a css file
 * has a Filter to show debug when it's being accessed.

The webapp `webapp-B/` :
 * has an index.html, with a reference to a dynamic css (a servlet)
 * has a CssServlet
   * set to load-on-startup
   * has an `init()` method that will use an HTTP request to webapp-A to fetch a CSS file
   * has a `doGet()` method that will return the cached CSS file

We have `jetty-base-test/` which is a `${jetty.base}` directory.
 * It has the modules `http`, `deploy`, and `webapp` enabled from the jetty-home
 * It has a new module `late-deploy` enabled from this repository
 * It has a new `webapps-late/` directory where we put WAR and XML files for late deployment

We have a `jetty-late-deploy/` module
 * It is responsible for waiting until the `Server` component reaches STARTED state and then triggering a deploy
 * Will use a pre-existing configuration of a `WebAppProvider` to perform the deployment
   (The existing `WebAppProvider` is used for all configurations except the monitored directory)
 * Will deploy the files in `webapps-late/` in alphabetical order
 * Will deploy only once, no hot-deploy feature is present.

## Using the jetty-base-test directory

The build should have created a `jetty-webapp-deployment-ordering/target/jetty-home-<version>` directory
which contains the unpacked `jetty-home` archive, we'll need to use this.

First, change into the `jetty-base-test/` directory, and then run Jetty.

``` shell
cd jetty-base-test
java -jar ../target/jetty-home-10.0.16/start.jar
```

The server is now running.

There are 4 URLs worth noting.

* http://localhost:8080/appA/css/main.css (the static CSS file, served from appA)
* http://localhost:8080/appA/index.html (and static index.html that uses the static CSS file)
* http://localhost:8080/appB/dyncss/main.css (the dynamically served CSS that was obtained during `CssServlet.init()`)
* http://localhost:8080/appB/index.html (the static index.html, using the dynamic CSS that was obtained during `CssServlet.init()`)


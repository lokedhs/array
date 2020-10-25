# KAP

An implementation of an APL-like language in Kotlin. The implementation is a Kotlin Multiplatform project,
and can be compiled to Java, natively and to Javascript.

## Summary

As of this writing, a majority of basic APL functionality is available, but some important features are not
yet available. The language also has several features that are unique to KAP, such as the ability to
define new syntax. Thanks to this, imperative commands such as `if` and `while` is implemented in the standard library
and are not part of the language itself. 

The language also implements first-class functions and closures. All of these features will be documented in a separate
document. However, this has not been written yet.

## Running the JavaFX based UI

The most feature-complete way to run KAP is to use the JVM client. It provides a nicer user interface, an editor,
highlighting of the location of errors, ability to draw graphics, and many other features.

To start the client, simply run:

```
./gradlew gui:run
```          

## Running the native version

Build the entire project:

```
./gradlew build
``` 

Once the build is complete, the binaries can be found in the following directory: `array/build/bin/linux`.

There is currently no support for native compilation on Windows or OSX. As long as the dependencies are available, it should be possible to
make it work. Help appreciated if anyone is interested in working on it.

## Web client

The application also compiles to Javascript and it's possible to run it in a browser. Note that a lot of functionality
is missing from the Javascript version. For example, the standard library cannot be loaded.

### Online demo

If you still want to try the web application you can try it here: http://kapdemo.dhsdevelopments.com/


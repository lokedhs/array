# KAP

An implementation of an APL-like language in Kotlin. The implementation is a Kotlin Multiplatform project, and can be compiled to Java,
natively and to Javascript.

## Summary

As of this writing, a majority of basic APL functionality is available, but some important features are not yet available. The language also
has several features that are unique to KAP, such as the ability to define new syntax. Thanks to this, imperative commands such as `if`
and `while` is implemented in the standard library and are not part of the language itself.

The language also implements first-class functions and closures. All of these features will be documented in a separate document. However,
this has not been written yet.

## What the language looks like

As a language based on APL, it uses non-ASCII symbols with most fundamental functions being a single character. This may make the language
seem complicated, but it's actually rather simple once one learns the general syntax.

The first line of code in the following example assigns a string to the variable A, and the second line splits the string at instances
of `-`, returning an array of arrays (a string in KAP is nothing more than a one-dimensional array of characters):

```
A ← "this-is-a-demo"
A ⊂⍨ A≠@-
```

Typing the above code into the REPL will display the following result:

```
┏━━━━━━━━━━━━━━━━━━━━━━┓
┃"this" "is" "a" "demo"┃
┗━━━━━━━━━━━━━━━━━━━━━━┛
```

The box around the result indicates that the result is an array. In this case it is a one-dimensional array with 4 elements, each of which
is an array of characters (i.e. string).

## Running the JavaFX based UI

The most feature-complete way to run KAP is to use the JVM client. It provides a nicer user interface, an editor, highlighting of the
location of errors, ability to draw graphics, and many other features.

To start the client, simply run:

```
./gradlew gui:run
```          

## Running the native version

Build the entire project:

```
./gradlew build
``` 

Once the build is complete, the binaries can be found in the following directory: `text-client/build/bin/linux`.

There is currently no support for native compilation on Windows or OSX. As long as the dependencies are available, it should be possible to
make it work. Help appreciated if anyone is interested in working on it.

## Web client

The application also compiles to Javascript, and it's possible to run it in a browser. Note that some functionality is missing from the
Javascript version. For example, network and file operations are currently not implemented.

### Online demo

If you still want to try the web application you can try it here: http://kapdemo.dhsdevelopments.com/

## Documentation

Currently there isn't much documentation available. This is a known limitation, and something that will be addressed. For the moment, anyone
interested in learning more can ask questions on the Matrix channel.

## Contributions

The main repository for this project is available from Github: https://github.com/lokedhs/array

For discussions about this project, feel free to join the [Matrix channel](https://matrix.to/#/#kap:dhsdevelopments.com):
`#kap:dhsdevelopments.com`.

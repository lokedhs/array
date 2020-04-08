# KAP - Array Language (not the final name)

An implementation of an APL-like language in Kotlin. The implementation is a Kotlin Multiplatform project,
and can be compiled to both Java and natively. It should also be possible to compile to Javascript but
this has not been tested.

## Summary

At this point, this project isn't usable, as a lot of fundamental features have not yet been implemented.

## Running the JavaFX based UI

Simply run:

```
./gradlew run
```          

## Running the native version

Build the entire project:

```
./gradlew build
``` 

Once the build is complete, the binaries can be found in the following directory: `array/build/bin/linux`.

It may be possible to compile it on Windows or OSX, but this has not been tested. Any testing on these
platforms is appreciated.

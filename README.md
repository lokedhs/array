# KAP

An implementation of an APL-like language in Kotlin. The implementation is a Kotlin Multiplatform project,
and can be compiled to both Java and natively. It should also be possible to compile to Javascript but
this has not been tested.

## Summary

At this point, this project isn't usable, as a lot of fundamental features have not yet been implemented.

## Running the JavaFX based UI

Simply run:

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
make it work.

Javascript compilation does not work, due to the fact that the Kotlin JS compiler does not support reflection. This is supposedly going to
change. Once reflection support is available, it should be possible to generate JS code.

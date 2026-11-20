# frida-java-bridge

Java runtime interop from Frida. This module is bundled with Frida and exposed
through the global named `Java`.

## Running the test-suite

### Android

#### Dependencies

- Android SDK Platform-Tools >= 29.0.6
- Android NDK r21+

With environment configured accordingly:

```sh
$ export ANDROID_SDK_ROOT=~/Library/Android/Sdk
$ export ANDROID_NDK_ROOT=/usr/local/opt/android-ndk-r25b
```

#### Configuration

 - Go to `/test/config.mk` for editing the device configuration settings.

#### Run

```sh
$ make check-android
```

#### Debug

```sh
$ make check-android-gdb
```

#### Auto-run tests on JavaScript change

```sh
$ make develop-android
```

### HotSpot JVM

With `JAVA_HOME` pointing to a HotSpot JDK (JDK 8–17 on Linux x86_64):

#### Run

```sh
$ make check-jvm
```

#### Debug

```sh
$ make check-jvm-gdb
```

#### Auto-run tests on JavaScript change

```sh
$ make develop-jvm
```

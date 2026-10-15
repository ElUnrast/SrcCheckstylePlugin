# SrcCheckstylePlugin

Gradle plugin that checks Java code against a fixed set of code and formatting
rules (based on [Checkstyle](https://checkstyle.org)) and can automatically
fix the safe violations.

## Features

- Applies the Checkstyle plugin with a bundled rule set (`checkstyle.xml`) and
  additional custom checks.
- `checkstyleFile`: runs Checkstyle for a single Java file.
- `checkstyleFixFile`: applies safe automatic fixes (whitespace, parentheses,
  blank lines, tabs, ...) to a single Java file.
- HTML and XML reports are enabled; violations do not fail the build.

## Usage

The plugin is provided via [JitPack](https://jitpack.io/#ElUnrast/SrcCheckstylePlugin).
Add this to the `settings.gradle` of the consuming build:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://jitpack.io' }
    }
    resolutionStrategy {
        eachPlugin {
            if( requested.id.id == 'de.src-gmbh.checkstyle' ) {
                useModule( 'com.github.ElUnrast:SrcCheckstylePlugin:v<version>' )
            }
        }
    }
}
```

and apply the plugin in the `build.gradle`:

```groovy
plugins {
    id 'de.src-gmbh.checkstyle'
}
```

Single file:

```
./gradlew checkstyleFile    -PcheckstyleFile=src/main/java/com/example/MyClass.java
./gradlew checkstyleFixFile -PcheckstyleFile=src/main/java/com/example/MyClass.java
```

## Build

Requires JDK 25 (Gradle toolchain).

```
./gradlew build
```

## License

Apache License 2.0, see [LICENSE](LICENSE).

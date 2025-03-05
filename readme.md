# ⚡︎ BoxLang Module: @MODULE_NAME@

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

This template can be used to create Ortus based BoxLang Modules. To use, just click the `Use this Template` button in the github repository: https://github.com/ortus-boxlang/boxlang-module-template and run the setup task from where you cloned it.

```bash
box task run taskFile=src/build/SetupTemplate
```

The `SetupTemplate` task will ask you for your module name, id and description and configure the template for you! Enjoy!

## Directory Structure

Here is a brief overview of the directory structure:

-   `.github/workflows` - These are the github actions to test and build the module via CI
-   `build` - This is a temporary non-sourced folder that contains the build assets for the module that gradle produces
-   `gradle` - The gradle wrapper and configuration
-   `src` - Where your module source code lives
-   `.cfformat.json` - A CFFormat using the Ortus Standards
-   `.editorconfig` - Smooth consistency between editors
-   `.gitattributes` - Git attributes
-   `.gitignore` - Basic ignores. Modify as needed.
-   `.markdownlint.json` - A linting file for markdown docs
-   `.ortus-java-style.xml` - Ortus Java Style for IntelliJ, VScode, Eclipse.
-   `box.json` - The box.json for your module used to publish to ForgeBox
-   `build.gradle` - The gradle build file for the module
-   `changelog.md` - A nice changelog tracking file
-   `CONTRIBUTING.md` - A contribution guideline
-   `gradlew` - The gradle wrapper
-   `gradlew.bat` - The gradle wrapper for windows
-   `ModuleConfig.cfc` - Your module's configuration. Modify as needed.
-   `readme.md` - Your module's readme. Modify as needed.
-   `settings.gradle` - The gradle settings file

Here is a brief overview of the source directory structure:

-   `build` - Build scripts and assets
-   `main` - The main module source code
    -   `bx` - The BoxLang source code
    -   `ModuleConfig.bx` - The BoxLang module configuration
        -   `bifs` - BoxLang built-in functions
        -   `components` - BoxLang components
        -   `config` - BoxLang configuration, schedulers, etc.
        -   `interceptors` - BoxLang interceptors
        -   `libs` - Java libraries to use that are NOT managed by gradle
        -   `models` - BoxLang models
    -   `java` - Java source code
    -   `resources` - Resources for the module placed in final jar
-   `test`
    -   `bx` - The BoxLang test code
    -   `java` - Java test code
    -   `resources` - Resources for testing
        -   `libs` - BoxLang binary goes here for now.

## Project Properties

The project name is defined in the `settings.gradle` file. You can change it there.
The project version, BoxLang Version and JDK version is defined in the `build.gradle` file. You can change it there.

## Gradle Tasks

Before you get started, you need to run the `downloadBoxLang` task in order to download the latest BoxLang binary until we publish to Maven.

```bash
gradle downloadBoxLang
```

This will store the binary under `/src/test/resources/libs` for you to use in your tests and compiler. Here are some basic tasks

| Task                | Description                                                                                                       |
| ------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `build`             | The default lifecycle task that triggers the build process, including tasks like `clean`, `assemble`, and others. |
| `clean`             | Deletes the `build` folders. It helps ensure a clean build by removing any previously generated artifacts.        |
| `compileJava`       | Compiles Java source code files located in the `src/main/java` directory                                          |
| `compileTestJava`   | Compiles Java test source code files located in the `src/test/java` directory                                     |
| `dependencyUpdates` | Checks for updated versions of all dependencies                                                                   |
| `downloadBoxLang`   | Downloads the latest BoxLang binary for testing                                                                   |
| `jar`               | Packages your project's compiled classes and resources into a JAR file `build/libs` folder                        |
| `javadoc`           | Generates the Javadocs for your project and places them in the `build/docs/javadoc` folder                        |
| `serviceLoader`     | Generates the ServiceLoader file for your project                                                                 |
| `spotlessApply`     | Runs the Spotless plugin to format the code                                                                       |
| `spotlessCheck`     | Runs the Spotless plugin to check the formatting of the code                                                      |
| `tasks`             | Show all the available tasks in the project                                                                       |
| `test`              | Executes the unit tests in your project and produces the reports in the `build/reports/tests` folder              |

## Tests

Please use the `src/test` folder for your unit tests. You can either test using TestBox o JUnit if it's Java.

## VSCode Tests

If you will be running tests for modules using the VSCode test explorer, then you need to make sure you remove the `/src/main/resources` line item from the configured class path, if not, the BoxLang core will try loading any service loaders it finds in that class path resolution.

> Please note, this IS ONLY FOR MODULE DEVELOPMENT.

Go to the `Java Projects` panel, click on the 3 dots and click on `Configure Classpath`. Remove the `/src/main/resources` line item and hit `APPLY SETTINGS` on the bottom left.

## Github Actions Automation

The github actions will clone, test, package, deploy your module to ForgeBox and the Ortus S3 accounts for API Docs and Artifacts. So please make sure the following environment variables are set in your repository.

> Please note that most of them are already defined at the org level

-   `FORGEBOX_TOKEN` - The Ortus ForgeBox API Token
-   `AWS_ACCESS_KEY` - The travis user S3 account
-   `AWS_ACCESS_SECRET` - The travis secret S3

> Please contact the admins in the `#infrastructure` channel for these credentials if needed

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12

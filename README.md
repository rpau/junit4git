![Logo](logo.png?raw=true&v=4&s=200)

[![Build Status](https://travis-ci.org/rpau/junit4git.svg?branch=master)](https://travis-ci.org/rpau/junit4git)
[![codecov](https://codecov.io/gh/rpau/smart-testing/branch/master/graph/badge.svg)](https://codecov.io/gh/rpau/smart-testing)

This is a JUnit extension that ignores those tests that are not related with 
your last changes in yourGit repository. This is not a new idea, since big companies, specially
with big mono-repos have worked on it.

[Martin Fowler](https://martinfowler.com/articles/rise-test-impact-analysis.html)
also describes how to use the test impact to infer the minimum tests to run.

You can use it from Maven and Gradle projects in any Junit based
project (written in any JVM based language: e.g Kotlin, Java, Scala)

## Getting Started
These instructions will get you a copy of the project up and running on your local machine.

### Requirements

- JRE 8
- Git

### Setup in your Maven Build

Declare a new test dependency in your pom.xml:
```xml
  <dependency>
    <groupId>org.walkmod</groupId>
    <artifactId>junit4git</artifactId>
    <version>1.0.3</version>
    <scope>test</scope>
  </dependency>
```
Add the following junit listener into your Maven tests executor:

 ```xml
 <plugin>
    <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-surefire-plugin</artifactId>
     <version>2.20.1</version>
     <configuration>
       <properties>
         <property>
           <name>listener</name>
           <value>org.walkmod.junit4git.junit4.Junit4GitListener</value>
         </property>
       </properties>
     </configuration>
  </plugin>
 ```

Commit these changes into your `master` branch (but you do not need to push them if you are evaluating the tool).

```bash
git checkout master
git add pom.xml
git commit -m 'junit4git setup'
```
### Generate a Base Testing Report

After having configured your build, run your `master` branch tests. 

```bash
mvn test
.
.
[WARNING] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
There is nothing new at this moment, but it has generated - as a hidden git note 
(`refs/notes/tests`).

Now, you can proceed to the next section (Run Tests Incrementally), or if 
you have curious mind and you want to check the contents of the generated report, 
run the following commands:

```bash
export GIT_NOTES_REF=refs/notes/tests
git notes show
```
A report, similar to the next one, will be printed in your console.

```json
[
  {
    "test": "CalculatorTest",
    "method": "testSum",
    "classes": [
      "Calculator"
     ]
  }
  ...
]
```

This report specifies classes that are instantiated for each one 
of your test methods.

### Run Tests Incrementally

After generating your base testing report, run the tests again (from the master branch or in a new local branch)

```bash
mvn tests
.
.
[WARNING] Tests run: 0, Failures: 0, Errors: 0, Skipped: 4
```

Voilà! You will see that all the tests have been skipped. 

However, if you add / modify 
new test files or modify an existing source file, only the affected tests are executed.

## License

This project is licensed under The Apache Software License.

## Build

This is a Gradle project that you can easily build running
```bash
gradle build
```

Or, if you want to generate a version to test it from a Maven project

```bash
gradle publishToMavenLocal
```

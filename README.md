junit4git: Skipping Unnecessary Junit Tests
==========================================================

This is a JUnit extension that resolves which, at runtime,
skip those tests that are not related with your last changes in your
Git repository.

You can use it from Maven and Gradle projects in any Junit based
project (written in any JVM based language: e.g Kotlin, Java, Scala)

## Requirements

- JRE 8
- Git

## Setup your Maven Build

Declare a new test dependency in your pom.xml:
```
  <dependency>
    <groupId>org.junit4git</groupId>
    <artifactId>junit4git</artifactId>
    <version>1.0</version>
    <scope>test</scope>
  </dependency>
```
Add the following junit listener into your Maven tests executor:

 ```
 <plugin>
    <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-surefire-plugin</artifactId>
     <version>2.20.1</version>
     <configuration>
       <properties>
         <property>
           <name>listener</name>
           <value>org.junit4git.junit4.Junit4GitListener</value>
         </property>
       </properties>
     </configuration>
   </plugin>
 ```

Commit these changes into your master` branch.

```
git checkout master
git add pom.xml
git commit -m 'junit4git setup'
```
## Generate a Base Testing Report

After having configured your build, run your `master` branch tests. 

```
mvn test
.

.
[WARNING] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
There is nothing new at this moment, but it has generated - as a hidden git note 
(`refs/notes/tests`) - a report about which sources are related with each one of your test methods.

If you want to check the generated report, run the following command:

```
export GIT_NOTES_REF=refs/notes/tests
git notes show
```

## Run Tests Incrementally

After generating your base testing report, run the tests again (from the master branch or in a new local branch)

```
mvn tests

.
.
[WARNING] Tests run: 0, Failures: 0, Errors: 0, Skipped: 4
```

Voilà! You will see that all the tests have been skipped. 

However, if you add / modify 
new test files or modify an existing source file, only the affected tests are executed.
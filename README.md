junit4git: Junit Extensions for an Incremental Test Execution
============================================================

This is a JUnit extension that resolves which, at runtime,
skip those tests that are not related with your last changes in your
Git repository.

You can use it from Maven and Gradle projects in any Junit based
project (written in any JVM based language: e.g Kotlin, Java, Scala)

## 1. Setup in a Maven Project

1. Declare a new test dependency in your pom.xml:
```
  <dependency>
    <groupId>org.junit4git</groupId>
    <artifactId>junit4git</artifactId>
    <version>1.0</version>
    <scope>test</scope>
  </dependency>
```
2. Add the following junit listener into your Maven tests executor:

 ```
 <plugin>
    <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-surefire-plugin</artifactId>
     <version>2.20.1</version>
     <configuration>
       <properties>
         <property>
           <name>listener</name>
           <value>org.junit4git.junit4.SmartTestListener</value>
         </property>
       </properties>
     </configuration>
   </plugin>
 ```

3. Commit these changes into your master` branch.

```
git checkout master
git add pom.xml
git commit -m 'junit4git setup'
```
## 2. Generate a Base Testing Report

Run your `master` branch tests. 

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

## 3. Run Tests Incrementally

5. Run the tests again (from the master branch or in a new local branch)

```
mvn tests

.
.
[WARNING] Tests run: 0, Failures: 0, Errors: 0, Skipped: 4
```

Voilà! You will see that all the tests have been skipped. 

However, if you add / modify 
new test files or modify an existing source file, only the affected tests are executed.
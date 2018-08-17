package org.walkmod.junit4git.javassist;

import javassist.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.walkmod.junit4git.core.reports.TestMethodReport;

import java.lang.instrument.Instrumentation;
import java.util.*;

import static org.mockito.Mockito.mock;

public class JavassistUtilsTest {

  @Test
  public void it_annotates_classes_when_appear_into_the_reports() throws Exception {
    ClassPool pool = ClassPool.getDefault();

    String testClass = "FooTestClass";
    String testMethod = "testFoo";

    CtClass evalClass = pool.makeClass(testClass);
    evalClass.addMethod(
            CtNewMethod.make(
                    "public void " + testMethod + "() { }",
                    evalClass));

    JavassistUtils javassist = new JavassistUtils();

    Map<String, List<TestMethodReport>> reports = new HashMap<>();

    reports.put(testClass, Arrays.asList(new TestMethodReport(testClass, testMethod, Collections.EMPTY_SET)));
    javassist.annotateMethods(Ignore.class, mock(Instrumentation.class), reports);

    Class clazz = evalClass.toClass();

    Assert.assertNotNull(clazz.getMethod(testMethod).getAnnotation(Ignore.class));
  }

  @Test
  public void it_respects_existing_annotations() throws Exception {
    ClassPool pool = ClassPool.getDefault();

    String testClass = "FooTestClass2";
    String testMethod = "testFoo2";
    JavassistUtils javassist = new JavassistUtils();

    CtClass evalClass = pool.makeClass(testClass);
    CtMethod method = CtNewMethod.make(
            "public void " + testMethod + "() { }",
            evalClass);
    evalClass.addMethod(method);

    javassist.annotateMethod(Test.class, testMethod, evalClass, evalClass.getClassFile().getConstPool());
    evalClass.defrost();

    Map<String, List<TestMethodReport>> reports = new HashMap<>();

    reports.put(testClass, Arrays.asList(new TestMethodReport(testClass, testMethod, Collections.EMPTY_SET)));
    javassist.annotateMethods(Ignore.class, mock(Instrumentation.class), reports);

    Class clazz = evalClass.toClass();

    Assert.assertNotNull(clazz.getMethod(testMethod).getAnnotation(Ignore.class));
    Assert.assertNotNull(clazz.getMethod(testMethod).getAnnotation(Test.class));
  }

  @Test
  public void it_ignores_classes_if_they_do_not_exists() {
    try {

      String testClass = "RemovedFooTestClass";
      String testMethod = "testFoo";

      JavassistUtils javassist = new JavassistUtils();

      Map<String, List<TestMethodReport>> reports = new HashMap<>();

      reports.put(testClass, Arrays.asList(new TestMethodReport(testClass, testMethod, Collections.EMPTY_SET)));
      javassist.annotateMethods(Ignore.class, mock(Instrumentation.class), reports);

    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void it_instruments_classes() throws Exception {

    JavassistUtils javassist = new JavassistUtils();
    ClassPool pool = ClassPool.getDefault();

    String instrumentedClass = "InstrumentedClass";
    String field = "x";

    CtClass evalClass = pool.makeClass(instrumentedClass);
    evalClass.addField(CtField.make("public static int " + field + ";", evalClass));
    evalClass.addConstructor(CtNewConstructor.defaultConstructor(evalClass));

    javassist.instrumentClassWithStaticStmt(instrumentedClass, field + " = 2;");

    Class clazz = pool.get(instrumentedClass).toClass();
    Object o = clazz.newInstance();
    Assert.assertEquals(2, o.getClass().getField(field).get(o));
  }

  @Test
  public void it_instruments_static_constructors() throws Exception {

    JavassistUtils javassist = new JavassistUtils();
    ClassPool pool = ClassPool.getDefault();

    String instrumentedClass = "InstrumentedClassWithoutMethods";
    String field = "x";

    CtClass evalClass = pool.makeClass(instrumentedClass);
    evalClass.addField(CtField.make("public static int " + field + ";", evalClass));

    javassist.instrumentClassWithStaticStmt(instrumentedClass, field + " = 2;");

    Class clazz = pool.get(instrumentedClass).toClass();
    Assert.assertEquals(2, clazz.getField(field).get(null));

  }

  @Test
  public void it_instruments_static_methods() throws Exception {
    JavassistUtils javassist = new JavassistUtils();
    ClassPool pool = ClassPool.getDefault();

    String instrumentedClass = "InstrumentedClassWithStaticMethods";
    String field = "x";

    CtClass evalClass = pool.makeClass(instrumentedClass);
    evalClass.addField(CtField.make("public static int " + field + ";", evalClass));

    CtMethod method = CtNewMethod.make(
            "public static int xValue() { return 2; }",
            evalClass);
    evalClass.addMethod(method);

    javassist.instrumentClassWithStaticStmt(instrumentedClass, field + " = 2;");

    Class clazz = pool.get(instrumentedClass).toClass();
    clazz.getMethod("xValue").invoke(null);
    Assert.assertEquals(2, clazz.getField(field).get(null));
  }

  @Test
  public void it_replaces_method_calls() throws Exception {
    JavassistUtils javassist = new JavassistUtils();
    ClassPool pool = ClassPool.getDefault();

    CtClass superClass = pool.makeClass("SuperClass");

    superClass.addField(CtField.make("public int x = 0;", superClass));

    superClass.addMethod(CtNewMethod.make(
            "public void test(String name) { x += 1; }",
            superClass));

    superClass.addMethod(CtNewMethod.make(
            "public void ignore(String name) { x += 2; }",
            superClass));

    superClass.addMethod(CtNewMethod.make(
            "public int xValue() { return x; }",
            superClass));

    String instrumentedClass = "InstrumentedClassWithChangedMethods";
    CtClass evalClass = pool.makeClass(instrumentedClass);
    evalClass.setSuperclass(superClass);

    evalClass.addConstructor(
            CtNewConstructor.make("public InstrumentedClassWithChangedMethods() { test(\"test1\"); test(\"test2\"); }",
                    evalClass));

    Class<?> parent = superClass.toClass();

    List<TestMethodReport> testsToIgnore = new LinkedList<>();

    testsToIgnore.add(new TestMethodReport("InstrumentedClassWithChangedMethods", "test1", null));
    testsToIgnore.add(new TestMethodReport("InstrumentedClassWithChangedMethods", "test2", null));

    javassist.replaceMethodCallOnConstructors("test", "ignore", evalClass, testsToIgnore);

    Class<?> clazz = evalClass.toClass();
    Object newInstance = clazz.newInstance();

    int x = (int) clazz.getMethod("xValue").invoke(newInstance);

    Assert.assertEquals(4, x);
  }

}

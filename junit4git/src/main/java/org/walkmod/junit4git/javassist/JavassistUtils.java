package org.walkmod.junit4git.javassist;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.reports.TestMethodReport;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;

public class JavassistUtils {

  private static Log log = LogFactory.getLog(JavassistUtils.class);

  public boolean annotateMethod(Class<?> annotationClass,
                             String md, CtClass clazz, ConstPool constpool) {
    try {
      CtMethod method = clazz.getDeclaredMethod(md);

      MethodInfo info = method.getMethodInfo();
      AnnotationsAttribute currentAttr = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);

      AnnotationsAttribute methodAttr = Optional.ofNullable(currentAttr).orElse(
              new AnnotationsAttribute(info.getConstPool(), AnnotationsAttribute.visibleTag));

      methodAttr.addAnnotation(new Annotation(annotationClass.getName(), constpool));
      method.getMethodInfo().addAttribute(methodAttr);
      return true;
    } catch (NotFoundException e) {
      //the method has been removed
      log.error("The method " + md + "does not exists in " + clazz.getName());
      return false;
    } catch (Exception e) {
      throw new RuntimeException("Error adding @" + annotationClass.getName() + " annotations", e);
    }
  }

  public void replaceMethodCallOnConstructors(String fromMethodName, String toMethodName, CtClass clazz) {

    CtConstructor[] constructors = clazz.getConstructors();
    for (CtConstructor constructor: constructors) {
      try {
        CodeConverter codeConverter = new CodeConverter();

        CtMethod oldMethod = Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(fromMethodName))
                .findFirst().get();

        CtMethod newMethod = Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(toMethodName))
                .findFirst().get();

        codeConverter.redirectMethodCall(oldMethod, newMethod);

        constructor.instrument(codeConverter);

      } catch (CannotCompileException e) {
        log.error("The constructor of " + clazz.getName() + " cannot be adapted to ignore tests", e);
      }
    }

  }

  public void annotateMethods(Class<?> annotationClass, Instrumentation inst, Map<String,
          List<TestMethodReport>> testsToMap) {
    annotateMethods(annotationClass, inst, ClassPool.getDefault(), testsToMap);
  }

  public void annotateMethods(Class<?> annotationClass, Instrumentation inst,
                              ClassPool pool, Map<String, List<TestMethodReport>> testsToMap) {

    Iterator<String> it = testsToMap.keySet().iterator();
    while (it.hasNext()) {
      String className = it.next();
      try {
        CtClass clazz = pool.get(className);
        ClassFile ccFile = clazz.getClassFile();
        ConstPool constpool = ccFile.getConstPool();
        testsToMap.get(className).stream()
                .map(TestMethodReport::getTestMethod)
                .forEach(md -> annotateMethod(annotationClass, md, clazz, constpool));

        clazz.defrost();
        inst.redefineClasses(new ClassDefinition(Class.forName(className), clazz.toBytecode()));
        log.info("The test class " + className + " will be ignored");
      } catch (NotFoundException | ClassNotFoundException e) {
        //the class has been removed
      } catch (Throwable e) {
        log.error("Error ignoring the test class " + className, e);
      }
    }
  }

  public byte[] instrumentClassWithStaticStmt(String className, String instrumentationInstruction)
          throws CannotCompileException, NotFoundException, IOException {
    ClassPool pool = ClassPool.getDefault();
    CtClass clazz = pool.get(className);

    if (clazz.isFrozen()) {
      clazz.defrost();
      clazz.detach();
    }

    for (CtConstructor ctConstructor : clazz.getConstructors()) {
      ctConstructor.insertAfter(instrumentationInstruction);
    }

    CtMethod[] methods = clazz.getMethods();
    if (methods != null) {
      for (CtMethod ctMethod : clazz.getMethods()) {
        if (Modifier.isStatic(ctMethod.getModifiers())) {
          ctMethod.insertAfter(instrumentationInstruction, true);
        }
      }
    }

    CtConstructor constructor = clazz.makeClassInitializer();
    constructor.insertBefore(instrumentationInstruction);

    return clazz.toBytecode();
  }
}

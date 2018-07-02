package org.walkmod.junit4git.javassist;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.reports.TestMethodReport;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JavassistUtils {

  private static Log log = LogFactory.getLog(JavassistUtils.class);

  public void annotateMethod(Class<?> annotationClass,
                             String md, CtClass clazz, ConstPool constpool) {
    try {
      CtMethod method = clazz.getDeclaredMethod(md);

      MethodInfo info = method.getMethodInfo();
      AnnotationsAttribute currentAttr = (AnnotationsAttribute) info.getAttribute(AnnotationsAttribute.visibleTag);

      AnnotationsAttribute methodAttr = Optional.ofNullable(currentAttr).orElse(
              new AnnotationsAttribute(info.getConstPool(), AnnotationsAttribute.visibleTag));

      methodAttr.addAnnotation(new Annotation(annotationClass.getName(), constpool));
      method.getMethodInfo().addAttribute(methodAttr);
    } catch (NotFoundException e) {
      //the method has been removed
    } catch (Exception e) {
      throw new RuntimeException("Error adding @" + annotationClass.getName() + " annotations", e);
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
        if (clazz.isFrozen()) {
          clazz.defrost();
          clazz.detach();
        }
        inst.redefineClasses(new ClassDefinition(Class.forName(className), clazz.toBytecode()));
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

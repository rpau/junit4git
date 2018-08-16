package org.walkmod.junit4git.core.bytecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.ignorers.TestIgnorer;
import org.walkmod.junit4git.core.reports.TestMethodReport;
import org.walkmod.junit4git.javassist.JavassistUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;


public class TestIgnorerTransformer implements ClassFileTransformer {


  private static Log log = LogFactory.getLog(TestIgnorerTransformer.class);

  private final TestIgnorer testIgnorer;

  private final  Map<String, List<TestMethodReport>> testsToMap;

  public TestIgnorerTransformer(TestIgnorer testIgnorer) throws Exception {
    this.testIgnorer = testIgnorer;
    testsToMap = testIgnorer.testsGroupedByClass();
    log.info("Last Test Impact Analysis: " + testsToMap.size() + " tests");
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer)
          throws IllegalClassFormatException {
    String name = normalizeName(className);
    try {
      if (testsToMap.containsKey(name)) {
        log.info("Ignoring " + name);
        return testIgnorer.ignoreTest(name, testsToMap.get(name));
      }
      return classfileBuffer;

    } catch (Exception e) {
      log.error("Error ignoring the tests of " + name, e);
      throw new IllegalClassFormatException("Error ignoring tests on " + name);
    }
  }

  private String normalizeName(String className) {
    String aux = className.replaceAll("/", "\\.");
    if (aux.endsWith(".class")) {
      aux = aux.substring(0, aux.length() - ".class".length());
    }
    return aux;
  }

}

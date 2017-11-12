package org.walkmod.junit4git.core.bytecode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.javassist.JavassistUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This class resolves all the loaded classes by means of instrumentation
 * techniques. The test report server uses it to generate the list of
 * loaded classes per test.
 */
public class AgentClassTransformer implements ClassFileTransformer {

  /**
   * Structure to store the list of referenced classes
   */
  private static Set<String> referencedClasses = new LinkedHashSet<>();

  private static String CLASS_EXTENSION = ".class";

  private static Log log = LogFactory.getLog(AgentClassTransformer.class);

  /**
   * Cleans the list of referenced classes. It is needed for clean the list of
   * referenced classes between tests.
   */
  public static void cleanUp() {
    referencedClasses = new LinkedHashSet<>();
  }

  /**
   * Stores a new reference. It is called by the application constructors.
   *
   * @param name full class name to store
   */
  public static void add(String name) {
    referencedClasses.add(name);
  }

  /**
   * Resolves if an specific class belongs to a third party library or to the
   * local project
   *
   * @param protectionDomain place where the class belongs to
   * @return if belongs to a third party library
   */
  protected boolean belongsToAJarFile(ProtectionDomain protectionDomain) {
    return Optional.of(protectionDomain.getCodeSource())
            .map(source -> source.getLocation().getPath().endsWith(".jar"))
            .orElse(true);
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer)
          throws IllegalClassFormatException {

    if (className != null && !belongsToAJarFile(protectionDomain)) {
      String normalizedName = normalizeName(className);
      return instrumentClass(normalizedName, classfileBuffer);
    }

    return classfileBuffer;
  }

  /**
   * Modifies the class constructors to report which is the loaded class.
   *
   * @param name            of the class to instrument
   * @param classfileBuffer the current binary representation to return
   *                        in case of modification exceptions
   * @return modified class bytecode
   */
  protected byte[] instrumentClass(String name, byte[] classfileBuffer) {
    try {
      return new JavassistUtils().instrumentClass(name,
              AgentClassTransformer.class.getName()
                      + ".add(\"" + name + "\");");
    } catch (Throwable e) {
      log.error("Error instrumenting " + name, e);
    }
    return classfileBuffer;
  }

  private String normalizeName(String className) {
    String aux = className.replaceAll("/", "\\.");
    if (aux.endsWith(CLASS_EXTENSION)) {
      aux = aux.substring(0, aux.length() - CLASS_EXTENSION.length());
    }
    return aux;
  }

  /**
   * @return the list of referenced classes during a program/test execution, which has been
   * resolved by instrumentation techniques
   */
  public static Set<String> getReferencedClasses() {
    return referencedClasses;
  }
}

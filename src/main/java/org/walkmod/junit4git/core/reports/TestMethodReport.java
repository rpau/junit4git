package org.walkmod.junit4git.core.reports;

import java.util.Set;

/**
 * Test method report which represents an executed tests and the loaded classes for that test.
 */
public class TestMethodReport {

  private final String test;
  private final String method;
  private final Set<String> classes;

  public TestMethodReport(String test, String method, Set<String> classes) {
    this.test = test;
    this.method = method;
    this.classes = classes;
  }

  /**
   * Class name that contain tests
   *
   * @return class name that contain tests
   */
  public String getTestClass() {
    return test;
  }

  /**
   * Test method
   *
   * @return test method
   */
  public String getTestMethod() {
    return method;
  }

  /**
   * Referenced classes by the test that has been loaded any point of its execution.
   * Classes that belong to third party libraries are excluded.
   *
   * @return Referenced classes by the test that has been loaded any point of its execution.
   */
  public Set<String> getReferencedClasses() {
    return classes;
  }

  /**
   * Test method identifier (class name + "#" + method name).
   * It is not allowed to have test methods with the same name under the same class.
   *
   * @return test method id.
   */
  public String getTestMethodId() {
    return getTestClass() + "#" + getTestMethod();
  }

  /**
   * Resolves if any of the referenced classes in the test report (and the test itself) are
   * impacted / referenced from a list of modified files.
   *
   * @param modifiedFiles to consider
   * @return if the test needs to be re-executed because it is impacted by a change.
   */
  public boolean isImpactedBy(Set<String> modifiedFiles) {
    return modifiedFiles.stream()
            .anyMatch(file -> !file.endsWith(".class")
                    &&
                    (matchesWithFile(getTestClass(), file) ||
                            getReferencedClasses().stream().anyMatch(clazz -> matchesWithFile(clazz, file))));
  }

  private boolean matchesWithFile(String clazz, String statusFile) {
    String testFilePath = toFilePath(getParentClassName(clazz));
    return fileWithoutExtension(statusFile).endsWith(testFilePath);
  }

  private String toFilePath(String name) {
    return name.replaceAll("\\.", "/");
  }

  private String fileWithoutExtension(String statusFile) {
    int index = statusFile.lastIndexOf(".");
    if (index > -1) {
      return statusFile.substring(0, index);
    }
    return statusFile;
  }

  private String getParentClassName(String className) {
    int innerClassIndex = className.indexOf("$");
    if (innerClassIndex > -1) {
      return className.substring(0, innerClassIndex);
    }
    return className;
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof TestMethodReport) {
      TestMethodReport aux = (TestMethodReport) o;
      return aux.test.equals(test)
              && aux.method.equals(method)
              && aux.classes.equals(classes);
    }
    return false;
  }
}

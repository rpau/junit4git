package org.walkmod.junit4git.core.reports;

import java.util.Set;

public class TestMethodReport {

    private final String test;
    private final String method;
    private final Set<String> classes;

    public TestMethodReport(String test, String method, Set<String> classes) {
        this.test = test;
        this.method = method;
        this.classes = classes;
    }

    public String getTestClass() {
        return test;
    }

    public String getTestMethod() {
        return method;
    }

    public Set<String> getReferencedClasses() {
        return classes;
    }

    public String getTestMethodId() {
      return getTestClass() + "#" + getTestMethod();
    }

    public boolean isImpactedBy(Set<String> modifiedFiles) {
      return modifiedFiles.stream()
              .anyMatch(file -> !file.endsWith(".class")
                      &&
                      (matchesWithFile(test, file) ||
                              classes.stream().anyMatch(clazz -> matchesWithFile(clazz, file))));
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

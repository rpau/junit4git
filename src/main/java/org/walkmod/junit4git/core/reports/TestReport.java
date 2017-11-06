package org.walkmod.junit4git.core.reports;

import java.util.Set;

public class TestReport {
    private final String test;
    private final String method;
    private final Set<String> classes;

    public TestReport(String test, String method, Set<String> classes) {
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof TestReport) {
            TestReport aux = (TestReport) o;
            return aux.test.equals(test)
                    && aux.method.equals(method)
                    && aux.classes.equals(classes);
        }
        return false;
    }
}

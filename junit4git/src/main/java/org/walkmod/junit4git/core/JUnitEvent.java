package org.walkmod.junit4git.core;

/**
 * Representation of the junit events that comes from a Junit listener.
 */
public class JUnitEvent {

  private final String eventType;

  private final String testClass;

  private final String testMethod;

  public JUnitEvent(String eventType, String testClass, String testMethod) {
    this.eventType = eventType;
    this.testClass = testClass;
    this.testMethod = testMethod;
  }

  public String getEventType() {
    return eventType;
  }

  public String getTestClass() {
    return testClass;
  }

  public String getTestMethod() {
    return testMethod;
  }
}

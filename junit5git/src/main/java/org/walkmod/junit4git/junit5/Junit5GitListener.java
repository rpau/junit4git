package org.walkmod.junit4git.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.walkmod.junit4git.core.JUnitEventType;
import org.walkmod.junit4git.core.TestsReportClient;

public class Junit5GitListener implements TestExecutionListener {

  private final TestsReportClient client;

  private TestPlan plan;

  public Junit5GitListener() {
    client = new TestsReportClient();
  }

  public void testPlanExecutionStarted(TestPlan testPlan) {
    this.plan = testPlan;
  }

  private String getClassName(TestIdentifier testIdentifier) {
    return plan.getParent(testIdentifier)
            .flatMap(TestIdentifier::getSource)
            .filter(ClassSource.class::isInstance)
            .map(ClassSource.class::cast)
            .map(ClassSource::getClassName)
            .orElseThrow(() -> new IllegalStateException("Invalid test class name"));
  }

  private String getMethodName(TestIdentifier testIdentifier) {
    return testIdentifier.getSource()
            .filter(MethodSource.class::isInstance)
            .map(MethodSource.class::cast)
            .map(MethodSource::getMethodName)
            .orElseThrow(() -> new IllegalStateException("Invalid test method name"));
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    if (testIdentifier.isTest()) {
      client.sendRequestToClassLoggerAgent(
              getClassName(testIdentifier),
              getMethodName(testIdentifier),
              JUnitEventType.START.getName());
    }
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    if (testIdentifier.isTest()) {
      client.sendRequestToClassLoggerAgent(
              getClassName(testIdentifier),
              getMethodName(testIdentifier),
              JUnitEventType.STOP.getName());
    }
  }
}

package org.walkmod.junit4git.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.walkmod.junit4git.core.JUnitEventType;
import org.walkmod.junit4git.core.TestsReportClient;

public class Junit4GitListener extends RunListener {

  private final TestsReportClient client;

  public Junit4GitListener() {
    this(new TestsReportClient());
  }

  public Junit4GitListener(TestsReportClient client) {
    this.client = client;
  }

  @Override
  public void testStarted(Description description) {
    client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(),
            JUnitEventType.START.getName());
  }

  @Override
  public void testFinished(Description description) {
    client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(),
            JUnitEventType.STOP.getName());
  }
}

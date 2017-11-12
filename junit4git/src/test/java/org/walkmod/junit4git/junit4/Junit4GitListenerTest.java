package org.walkmod.junit4git.junit4;

import org.junit.Test;
import org.junit.runner.Description;
import org.walkmod.junit4git.core.JUnitEventType;
import org.walkmod.junit4git.core.TestsReportClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Junit4GitListenerTest {

  @Test
  public void when_a_test_starts_then_it_is_notified() {
    TestsReportClient client = mock(TestsReportClient.class);

    Junit4GitListener listener = new Junit4GitListener(client);
    listener.testStarted(Description.createTestDescription("TestClass", "testMethod"));
    verify(client).sendRequestToClassLoggerAgent(
            "TestClass", "testMethod", JUnitEventType.START.getName());
  }

  @Test
  public void when_a_test_finishes_then_it_is_notified() {
    TestsReportClient client = mock(TestsReportClient.class);

    Junit4GitListener listener = new Junit4GitListener(client);
    listener.testFinished(Description.createTestDescription("TestClass", "testMethod"));
    verify(client).sendRequestToClassLoggerAgent(
            "TestClass", "testMethod", JUnitEventType.STOP.getName());
  }
}

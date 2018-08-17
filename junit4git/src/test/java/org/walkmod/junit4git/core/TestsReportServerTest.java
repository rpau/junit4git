package org.walkmod.junit4git.core;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.bytecode.AgentClassTransformer;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.ReportStatus;
import org.walkmod.junit4git.core.reports.TestMethodReport;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestsReportServerTest {

  @Test
  public void test_when_a_start_event_arrives_the_referenced_classes_of_the_previous_test_are_clean() throws Exception {
    AgentClassTransformer.add("Foo");
    AbstractTestReportStorage storage = mock(AbstractTestReportStorage.class);
    when(storage.prepare()).thenReturn(ReportStatus.CLEAN);

    TestsReportServer server = new TestsReportServer(storage);
    server.process(new JUnitEvent(JUnitEventType.START.getName(), "FooClass", "test_method"));
    Assert.assertEquals(Collections.EMPTY_SET, AgentClassTransformer.getReferencedClasses());
  }

  @Test
  public void test_when_a_stop_event_arrives_the_test_is_stored() throws Exception {
    AbstractTestReportStorage storage = mock(AbstractTestReportStorage.class);
    when(storage.prepare()).thenReturn(ReportStatus.CLEAN);
    TestsReportServer server = new TestsReportServer(storage);
    server.process(new JUnitEvent(JUnitEventType.STOP.getName(), "FooClass", "test_method"));
    verify(storage).appendTestReport(new TestMethodReport("FooClass", "test_method",
            AgentClassTransformer.getReferencedClasses()));
  }
}

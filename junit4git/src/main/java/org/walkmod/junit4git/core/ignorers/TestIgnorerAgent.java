package org.walkmod.junit4git.core.ignorers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.bytecode.TestIgnorerTransformer;
import org.walkmod.junit4git.core.reports.GitTestReportStorage;
import org.walkmod.junit4git.core.reports.ReportStatus;

import java.lang.instrument.Instrumentation;

public class TestIgnorerAgent {

  private static Log log = LogFactory.getLog(TestIgnorerAgent.class);

  public static void premain(String args, Instrumentation instrumentation) {
    log.info("JUnit4Git agent started");
    try {
      GitTestReportStorage storage = new GitTestReportStorage();
      ReportStatus status = storage.getStatus();
      if (status.equals(ReportStatus.DIRTY)) {
        instrumentation.addTransformer(new TestIgnorerTransformer(new TestIgnorer(new GitTestReportStorage())));
      }
    } catch (Exception e) {
      log.error(e);
    }
  }
}

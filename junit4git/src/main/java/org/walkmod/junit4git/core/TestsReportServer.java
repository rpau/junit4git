package org.walkmod.junit4git.core;

import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.bytecode.AgentClassTransformer;
import org.walkmod.junit4git.core.bytecode.TestIgnorerTransformer;
import org.walkmod.junit4git.core.ignorers.TestIgnorer;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.GitTestReportStorage;
import org.walkmod.junit4git.core.reports.ReportStatus;
import org.walkmod.junit4git.core.reports.TestMethodReport;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is a Server that generates receives the affected classes for each
 * single test and stores it in a report.
 * <p>
 * Moreover, this class also is a Java agent, which allows to mark the unaffected tests
 * by our local changes (if there is any) with the @Ignore annotation.
 */
public class TestsReportServer extends NanoHTTPD {

  private final Map<String, String> input = new HashMap<>();

  private final AbstractTestReportStorage storage;

  /*Transformer to report which classes are used from which tests*/
  private final AgentClassTransformer usageTransformer;

  /*Transformer to ignore tests that are not related with the last changes*/
  private final TestIgnorerTransformer ignorerTransformer;

  private static Gson gson = new Gson();

  private static Log log = LogFactory.getLog(TestsReportServer.class);

  private static int PORT = 9000;

  private final ReportStatus status;

  private final boolean fromRunner;

  public TestsReportServer() throws Exception {
    this(false);
  }

  public TestsReportServer(boolean fromRunner) throws Exception {
    this(new GitTestReportStorage(), fromRunner, PORT);
  }

  public TestsReportServer(AbstractTestReportStorage storage, boolean fromRunner, int port) throws Exception {
    this(storage, new AgentClassTransformer(),
            new TestIgnorerTransformer(new TestIgnorer(storage)), fromRunner, port);
  }

  public TestsReportServer(AbstractTestReportStorage storage) throws Exception {
    this(storage, new AgentClassTransformer(), new TestIgnorerTransformer(new TestIgnorer(storage)), false, PORT);
  }

  public TestsReportServer(AbstractTestReportStorage storage, AgentClassTransformer usageTransformer,
                           TestIgnorerTransformer ignorerTransformer, boolean fromRunner, int port) {
    super(port);
    this.storage = storage;
    this.usageTransformer = usageTransformer;
    status = storage.prepare();
    this.ignorerTransformer = ignorerTransformer;
    this.fromRunner = fromRunner;
  }


  protected void readInput(IHTTPSession session) {
    try {
      session.parseBody(input);
    } catch (Exception e) {
      log.error("Error parsing the request", e);
    }
  }

  /**
   * It marks all the tests that can be ignored before running any test.
   * When there are no changes in the repo pending to push in the master branch,
   * it will run all the tests. Otherwise, it will mark the oldest ones as ignored.
   *
   * @param inst object used to reload modified classes with the @Ignore annotation
   * @throws Exception
   */
  protected void transformClasses(Instrumentation inst) throws Exception {
    if (status.equals(ReportStatus.CLEAN)) {
      inst.addTransformer(usageTransformer);
    } else {
      if (!fromRunner) {
        inst.addTransformer(ignorerTransformer);
      } else {
        log.debug("Skipping test ignorer because Junit4Git agent is already running");
      }
    }
  }

  @Override
  public Response serve(IHTTPSession session) {
    readInput(session);
    process(gson.fromJson(input.get("postData"), JUnitEvent.class));
    return newFixedLengthResponse("");
  }

  protected void process(JUnitEvent event) {
    if (status.equals(ReportStatus.CLEAN)) {
      Set<String> referencedClasses = AgentClassTransformer.getReferencedClasses();
      if (JUnitEventType.START.getName().equals(event.getEventType())) {
        AgentClassTransformer.cleanUp();
      } else {
        storage.appendTestReport(new TestMethodReport(
                event.getTestClass(), event.getTestMethod(), referencedClasses));
      }
    }
  }

  /**
   * It launches dynamically an agent to instrument all the classes before starting any test.
   * @param agentArgs
   * @param inst
   * @throws Exception
   */
  public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
    try {

      TestsReportServer agent = new TestsReportServer(agentArgs.contains("--fromRunner"));
      agent.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
      agent.transformClasses(inst);
    } catch (Exception e) {
      log.error("Error starting the embedded Junit4Git agent", e);
    }
  }
}

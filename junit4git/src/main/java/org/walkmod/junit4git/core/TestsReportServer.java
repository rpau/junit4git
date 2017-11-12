package org.walkmod.junit4git.core;

import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.walkmod.junit4git.core.bytecode.AgentClassTransformer;
import org.walkmod.junit4git.core.ignorers.TestIgnorer;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.GitTestReportStorage;
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

  private final AgentClassTransformer transformer;

  private static Gson gson = new Gson();

  private static Log log = LogFactory.getLog(TestsReportServer.class);

  private static int PORT = 9000;

  public TestsReportServer() {
    this(PORT);
  }

  public TestsReportServer(int port) {
    this(new GitTestReportStorage(), new AgentClassTransformer(), port);
  }

  public TestsReportServer(AbstractTestReportStorage storage) {
    this(storage, new AgentClassTransformer(), PORT);
  }

  public TestsReportServer(AbstractTestReportStorage storage, AgentClassTransformer transformer, int port) {
    super(port);
    this.storage = storage;
    this.transformer = transformer;
    storage.prepare();
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
  protected void ignoreTests(Instrumentation inst) throws Exception {
    inst.addTransformer(transformer);
    new TestIgnorer(storage).ignoreTests(inst);
  }

  @Override
  public Response serve(IHTTPSession session) {
    readInput(session);
    process(gson.fromJson(input.get("postData"), JUnitEvent.class));
    return newFixedLengthResponse("");
  }

  protected void process(JUnitEvent event) {
    Set<String> referencedClasses = AgentClassTransformer.getReferencedClasses();
    if (JUnitEventType.START.getName().equals(event.getEventType())) {
      AgentClassTransformer.cleanUp();
    } else {
      storage.appendTestReport(new TestMethodReport(
              event.getTestClass(), event.getTestMethod(), referencedClasses));
    }
  }

  /**
   * It launches dynamically an agent to instrument all the classes before starting any test.
   * @param agentArgs
   * @param inst
   * @throws Exception
   */
  public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
    TestsReportServer agent = new TestsReportServer();
    agent.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    agent.ignoreTests(inst);
  }
}

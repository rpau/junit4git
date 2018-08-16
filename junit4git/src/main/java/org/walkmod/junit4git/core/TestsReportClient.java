package org.walkmod.junit4git.core;


import com.ea.agentloader.AgentLoader;
import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Client API for TestsReportServer used from a Junit listener to notify about
 * the execution status of the junit tests.
 */
public class TestsReportClient {

  private final OkHttpClient client;

  private static Boolean loaded = false;

  private static Gson gson = new Gson();

  private static Log log = LogFactory.getLog(TestsReportClient.class);

  private static final String SERVER_TEST_REPORT_URL = "http://localhost:9000";

  public TestsReportClient() {
    this(false);
  }

  public TestsReportClient(boolean fromRunner) {
    this(new OkHttpClient(), !loaded, fromRunner);
  }

  public TestsReportClient(OkHttpClient client, boolean startupServer, boolean fromRunner) {
    this.client = client;
    if (startupServer) {
      startUpAgentServer(fromRunner);
      System.out.println("Junit4Git started [SUCCESS]");
    }
  }

  protected void startUpAgentServer(boolean fromRunner) {
    String options = "";
    if (fromRunner) {
      options += "--fromRunner";
    }
    AgentLoader.loadAgentClass(TestsReportServer.class.getName(), options);
    loaded = true;
  }

  public void sendRequestToClassLoggerAgent(String className, String methodName, String event) {
    try {
      RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
              gson.toJson(new JUnitEvent(event, className, methodName)));

      Request request = new Request.Builder()
              .url(SERVER_TEST_REPORT_URL)
              .post(body)
              .build();

      client.newCall(request).execute();

    } catch (Exception e) {
      log.error("Error sending the test request to the server", e);
    }
  }
}

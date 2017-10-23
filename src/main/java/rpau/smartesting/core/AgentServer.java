package rpau.smartesting.core;

import com.google.gson.*;
import fi.iki.elonen.NanoHTTPD;
import rpau.smartesting.core.ignorers.TestIgnorer;
import rpau.smartesting.core.reports.AbstractReportUpdater;
import rpau.smartesting.core.reports.GitNotesReportUpdater;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public class AgentServer extends NanoHTTPD {

    private final Map<String, String> input = new HashMap<>();

    private final AbstractReportUpdater service;

    public AgentServer(int port) throws IOException {
        super(port);
        service = new GitNotesReportUpdater();
        service.removeContents();
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            session.parseBody(input);
        } catch (Exception e) {
        }
        JsonObject request = new JsonParser().parse(input.get("postData")).getAsJsonObject();
        service.update(request.get("event").getAsString(),
                request.get("testClass").getAsString(),
                request.get("testMethod").getAsString());
        return newFixedLengthResponse("");
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        AgentServer agent = new AgentServer(9000);
        agent.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        inst.addTransformer(new AgentClassTransformer());
        new TestIgnorer(agent.service).ignoreTests(inst);
    }
}

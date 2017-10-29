package org.junit4git.core;

import com.google.gson.*;
import fi.iki.elonen.NanoHTTPD;
import org.junit4git.core.reports.AbstractReportUpdater;
import org.junit4git.core.reports.GitNotesReportUpdater;
import org.junit4git.core.ignorers.TestIgnorer;

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

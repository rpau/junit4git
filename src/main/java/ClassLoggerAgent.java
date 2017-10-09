import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fi.iki.elonen.NanoHTTPD;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

public class ClassLoggerAgent extends NanoHTTPD {

    private final LoggerClassTransformer transformer;

    private final Map<String, String> input = new HashMap<>();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private FileWriter writer;

    public ClassLoggerAgent(int port, LoggerClassTransformer transformer) throws IOException {
        super(port);
        this.transformer = transformer;

        File report = new File("smart-testing-report.json");
        writer = new FileWriter(report);
    }


    @Override
    public Response serve(IHTTPSession session) {
        try {
            session.parseBody(input);
        } catch (Exception e) {
        }
        Map<String, String> parms = session.getParms();

        if ("start".equals(parms.get("event"))) {
            transformer.createContext();
        } else {
            JsonArray array = transformer.destroyContext();
            JsonObject object = new JsonObject();
            object.addProperty("test", parms.get("testClass"));
            object.addProperty("method", parms.get("testMethod"));
            object.add("classes", array);
            try {
                writer.write(gson.toJson(object));
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return newFixedLengthResponse("");
    }

    @Override
    public void closeAllConnections() {
        super.closeAllConnections();
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws IOException {
        ClassLoggerAgent agent = new ClassLoggerAgent(9000, new LoggerClassTransformer());
        agent.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        inst.addTransformer(agent.transformer);
    }
}

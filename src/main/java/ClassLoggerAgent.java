import com.google.gson.*;
import fi.iki.elonen.NanoHTTPD;
import resolvers.LocalTestsResolver;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassLoggerAgent extends NanoHTTPD {

    private final LoggerClassTransformer transformer;

    private final Map<String, String> input = new HashMap<>();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File report = new File("smart-testing-report.json");

    public ClassLoggerAgent(int port, LoggerClassTransformer transformer) throws IOException {
        super(port);
        this.transformer = transformer;
        clearFile();
    }

    private void clearFile() {
        try (FileWriter writer = new FileWriter(report)){
            writer.write("");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Response serve(IHTTPSession session) {
        try {
            session.parseBody(input);
        } catch (Exception e) {
        }
        JsonArray array = transformer.destroyContext();
        JsonObject json = new JsonParser().parse(input.get("postData")).getAsJsonObject();

        if ("start".equals(json.get("event").getAsString())) {
            transformer.createContext();
        } else {
            JsonArray tests = new JsonArray();


            if (report.exists()) {
                try {
                    tests = new JsonParser().parse(
                            new InputStreamReader(new FileInputStream(report)))
                            .getAsJsonArray();
                } catch (Exception e) {}
            }

            JsonObject object = new JsonObject();
            object.addProperty("test", json.get("testClass").getAsString());
            object.addProperty("method", json.get("testMethod").getAsString());
            object.add("classes", array);
            tests.add(object);
            try (FileWriter writer = new FileWriter(report)){
                writer.write(gson.toJson(tests));
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
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        ClassLoggerAgent agent = new ClassLoggerAgent(9000, new LoggerClassTransformer());
        agent.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        inst.addTransformer(agent.transformer);
        new LocalTestsResolver().ignoreTests(inst);
    }
}

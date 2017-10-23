package rpau.smartesting.core;


import com.ea.agentloader.AgentLoader;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class AgentClient {

    private OkHttpClient client = new OkHttpClient();

    private static Boolean loaded = false;

    public AgentClient() {
        if (!loaded) {
            AgentLoader.loadAgentClass(AgentServer.class.getName(), "");
            loaded = true;
        }
    }

    public void sendRequestToClassLoggerAgent(String className, String methodName, String event) {
        try {

            JsonObject object = new JsonObject();
            object.addProperty("event", event);
            object.addProperty("testClass", className);
            object.addProperty("testMethod", methodName);

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), object.toString());

            Request request = new Request.Builder()
                    .url("http://localhost:9000")
                    .post(body)
                    .build();

            client.newCall(request).execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

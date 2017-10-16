package rpau.smartesting.core;

import com.ea.agentloader.AgentLoader;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class JunitLoggerListener extends RunListener {

    private OkHttpClient client = new OkHttpClient();

    private static Boolean loaded = false;

    public JunitLoggerListener() throws Exception {
        if (!loaded) {
            AgentLoader.loadAgentClass(ClassLoggerAgent.class.getName(), "");
            loaded = true;
        }
    }

    private void send(Description description, String event) {
        try {

            JsonObject object = new JsonObject();
            object.addProperty("event", event);
            object.addProperty("testClass", description.getClassName());
            object.addProperty("testMethod", description.getMethodName());

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

    @Override
    public void testStarted(Description description) {
        send(description, "start");
    }

    @Override
    public void testFinished(Description description) {
        send(description, "stop");
    }
}

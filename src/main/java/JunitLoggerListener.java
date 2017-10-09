import com.ea.agentloader.AgentLoader;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.List;


public class JunitLoggerListener extends RunListener {

    OkHttpClient client = new OkHttpClient();

    public JunitLoggerListener() {
        AgentLoader.loadAgentClass(ClassLoggerAgent.class.getName(), "");
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

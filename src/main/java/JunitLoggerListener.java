import com.ea.agentloader.AgentLoader;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

import java.util.ArrayList;
import java.util.List;


public class JunitLoggerListener extends RunListener {

    public JunitLoggerListener() {
        AgentLoader.loadAgentClass(ClassLoggerAgent.class.getName(), "");
    }

    private void send(Description description, String event) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://localhost:9000");
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("event", event));
            urlParameters.add(new BasicNameValuePair("testClass", description.getClassName()));
            urlParameters.add(new BasicNameValuePair("testMethod", description.getMethodName()));
            httppost.setEntity(new UrlEncodedFormEntity(urlParameters));
            httpclient.execute(httppost);
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

package rpau.smartesting.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import rpau.smartesting.core.AgentClient;

public class SmartTestListener extends RunListener {

    private AgentClient client = new AgentClient();

    @Override
    public void testStarted(Description description) {
        client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(), "start");
    }

    @Override
    public void testFinished(Description description) {
        client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(), "stop");
    }
}

package org.junit4git.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit4git.core.AgentClient;

public class Junit4GitListener extends RunListener {

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

package org.walkmod.junit4git.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.walkmod.junit4git.core.JUnitEventType;
import org.walkmod.junit4git.core.TestsReportClient;

public class Junit4GitListener extends RunListener {

    private TestsReportClient client = new TestsReportClient();

    @Override
    public void testStarted(Description description) {
        client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(),
                JUnitEventType.START.getName());
    }

    @Override
    public void testFinished(Description description) {
        client.sendRequestToClassLoggerAgent(description.getClassName(), description.getMethodName(),
                JUnitEventType.STOP.getName());
    }
}

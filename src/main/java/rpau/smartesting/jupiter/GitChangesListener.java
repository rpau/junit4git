package rpau.smartesting.jupiter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import rpau.smartesting.core.AgentClient;

public class GitChangesListener implements TestExecutionListener {

    private AgentClient client = new AgentClient();

    public void executionStarted(TestIdentifier testIdentifier) {
        client.sendRequestToClassLoggerAgent(testIdentifier.getType().name(),
                testIdentifier.getDisplayName(),
                "start");
    }

    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        client.sendRequestToClassLoggerAgent(testIdentifier.getType().name(),
                testIdentifier.getDisplayName(),
                "stop");
    }

}

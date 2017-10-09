import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class LoggerRunner extends BlockJUnit4ClassRunner {

    public LoggerRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.addListener(new JunitLoggerListener());
        notifier.fireTestRunStarted(getDescription());
        super.run(notifier);
    }
}
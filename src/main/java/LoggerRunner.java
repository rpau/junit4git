import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class LoggerRunner extends BlockJUnit4ClassRunner {

    private static JunitLoggerListener listener = null;

    static {
        try {
            listener = new JunitLoggerListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LoggerRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier) {
        if (listener != null) {
            notifier.addListener(listener);
            listener = null;
        }
        notifier.fireTestRunStarted(getDescription());
        super.run(notifier);
    }
}
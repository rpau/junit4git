package rpau.smartesting.junit4;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class SmartTestRunner extends BlockJUnit4ClassRunner {

    private static SmartTestListener listener = null;

    static {
        try {
            listener = new SmartTestListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SmartTestRunner(Class<?> klass) throws InitializationError {
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
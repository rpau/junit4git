package org.junit4git.junit4;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class Junit4GitRunner extends BlockJUnit4ClassRunner {

    private static Junit4GitListener listener = null;

    static {
        try {
            listener = new Junit4GitListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Junit4GitRunner(Class<?> klass) throws InitializationError {
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
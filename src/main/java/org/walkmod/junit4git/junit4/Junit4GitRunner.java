package org.walkmod.junit4git.junit4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class Junit4GitRunner extends BlockJUnit4ClassRunner {

  private static Junit4GitListener listener = null;

  private static Log log = LogFactory.getLog(Junit4GitRunner.class);

  static {
    try {
      listener = new Junit4GitListener();
    } catch (Exception e) {
      log.error("Error launching the Runner ", e);
    }
  }

  public Junit4GitRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  protected void setUpListener(RunNotifier notifier) {
    if (listener != null) {
      notifier.addListener(listener);
      listener = null;
    }
  }

  @Override
  public void run(RunNotifier notifier) {
    setUpListener(notifier);
    notifier.fireTestRunStarted(getDescription());
    super.run(notifier);
  }
}
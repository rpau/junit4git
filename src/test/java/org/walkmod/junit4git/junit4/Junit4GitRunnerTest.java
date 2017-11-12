package org.walkmod.junit4git.junit4;

import org.junit.Test;
import org.junit.runner.notification.RunNotifier;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Junit4GitRunnerTest {

  @Test
  public void it_updates_notifier() throws Exception {
    Junit4GitRunner runner = new Junit4GitRunner(Foo.class);
    RunNotifier notifier = mock(RunNotifier.class);
    runner.run(notifier);
    verify(notifier).fireTestRunStarted(any());
  }

  public static class Foo {
    @Test
    public void test() {
    }
  }
}

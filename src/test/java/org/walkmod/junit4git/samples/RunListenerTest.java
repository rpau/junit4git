package org.walkmod.junit4git.samples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.walkmod.junit4git.junit4.Junit4GitRunner;


@RunWith(Junit4GitRunner.class)
public class RunListenerTest {

  @Test
  public void testMethod() throws Exception {
    Hello h = new Hello();
  }

  @Test
  public void testMethod2() throws Exception {
    Hello h = new Hello();
  }

}
package rpau.smartesting.samples;

import org.junit.Test;
import org.junit.runner.RunWith;
import rpau.smartesting.core.LoggerRunner;


@RunWith(LoggerRunner.class)
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
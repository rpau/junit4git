package rpau.smartesting.jupiter;

import org.junit.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import rpau.smartesting.jupiter.samples.HelloTest;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;



public class GitChangesTransformerTest {

    @Test
    public void test() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectClass(HelloTest.class)
                )
                .build();

        Launcher launcher = LauncherFactory.create();

        GitChangesListener listener = new GitChangesListener();
        launcher.execute(request, listener);
    }
}

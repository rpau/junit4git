package rpau.smartesting.resolvers;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class DefaultIgnoredTestsResolver extends AbstractIgnoredTestsResolver {

    private File report = new File("smart-testing-report.base.json");

    public DefaultIgnoredTestsResolver() {
    }

    public DefaultIgnoredTestsResolver(File report) {
        this.report = report;
    }

    @Override
    public Set<String> getTestsToIgnore() throws IOException, GitAPIException {
        if (!report.exists()) {
            return new HashSet<>();
        }
        return getTestsToIgnore(new FileInputStream(report));
    }

}

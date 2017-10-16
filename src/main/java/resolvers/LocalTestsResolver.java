package resolvers;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.Set;

public class LocalTestsResolver extends TestsResolver {

    private File report = new File("smart-testing-report.base.json");

    public LocalTestsResolver() {
    }

    public LocalTestsResolver(File report) {
        this.report = report;
    }

    @Override
    public Set<String> getTestsToIgnore() throws IOException, GitAPIException {
        return getTestsToIgnore(new FileInputStream(report));
    }

}

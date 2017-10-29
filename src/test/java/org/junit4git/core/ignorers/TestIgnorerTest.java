package org.junit4git.core.ignorers;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit4git.core.reports.FileReportUpdater;
import org.junit4git.junit4.Junit4GitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@RunWith(Junit4GitRunner.class)
public class TestIgnorerTest {

    @Test
    public void whenThereAreNoTestsThenNothingIsIgnored() throws Exception {
        TestIgnorer resolver = new TestIgnorer(new FileReportUpdater()){
            @Override
            protected Set<String> runGitStatus() throws IOException, GitAPIException {
                return Collections.EMPTY_SET;
            }
        };
        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream("[]", Charset.forName("UTF-8")));
        Assert.assertEquals(Collections.EMPTY_SET, result);
    }

    @Test
    public void whenThereIsATestWhichIsNotModifiedThenIsIgnored() throws Exception {
        FileReportUpdater updater = new FileReportUpdater() {
            @Override
            public InputStream getBaseReport() {
                return IOUtils.toInputStream(
                        "[{\n" +
                                "  \"test\": \"MyTest\",\n" +
                                "  \"method\": \"testMethod\",\n" +
                                "  \"classes\": [\n" +
                                "    \"Hello\"\n" +
                                "  ]\n" +
                                "}]", Charset.forName("UTF-8"));
            }
        };

        TestIgnorer resolver = new TestIgnorer(updater){
            @Override
            protected Set<String> runGitStatus() throws IOException, GitAPIException {
                return Collections.EMPTY_SET;
            }
        };
        Set<String> result = resolver.getTestsToIgnore(updater.getBaseReport());
        Set<String> expected = new HashSet<>(Arrays.asList("MyTest#testMethod"));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void whenThereIsATestWhichIsModifiedThenIsNotIgnored() throws Exception {
        TestIgnorer resolver = new TestIgnorer(new FileReportUpdater()){
            @Override
            protected Set<String> runGitStatus() throws IOException, GitAPIException {
                return new HashSet<>(Arrays.asList("MyTest.java"));
            }
        };
        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream(
                "[{\n" +
                        "  \"test\": \"MyTest\",\n" +
                        "  \"method\": \"testMethod\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello\"\n" +
                        "  ]\n" +
                        "}]", Charset.forName("UTF-8")));

        Assert.assertEquals(Collections.EMPTY_SET, result);
    }

    @Test
    public void whenThereIsARefClassWhichIsModifiedThenIsNotIgnored() throws Exception {
        TestIgnorer resolver = new TestIgnorer(new FileReportUpdater()){
            @Override
            protected Set<String> runGitStatus() throws IOException, GitAPIException {
                return new HashSet<>(Arrays.asList("org/junit4git/samples/Hello.java"));
            }
        };
        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream(
                "[{\n" +
                        "  \"test\": \"MyTest\",\n" +
                        "  \"method\": \"testMethod\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello\"\n" +
                        "  ]\n" +
                        "}]", Charset.forName("UTF-8")));

        Assert.assertEquals(Collections.EMPTY_SET, result);
    }

}

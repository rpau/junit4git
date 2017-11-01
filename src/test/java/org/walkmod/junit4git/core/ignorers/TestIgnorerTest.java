package org.walkmod.junit4git.core.ignorers;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.reports.AbstractReportUpdater;
import org.walkmod.junit4git.core.reports.FileReportUpdater;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestIgnorerTest {

    private static class MockedTestIgnorer extends TestIgnorer {

        private Set<String> files;

        public MockedTestIgnorer(AbstractReportUpdater updater) {
            this(Collections.EMPTY_SET, updater);
        }

        public MockedTestIgnorer(Set<String> files, AbstractReportUpdater updater) {
            super(updater);
            this.files = files;
        }

        @Override
        protected Set<String> runGitStatus() throws IOException, GitAPIException {
            return files;
        }

        @Override
        protected Set<String> getUpdatesFromTheBaseBranch() throws IOException, GitAPIException {
            return new HashSet<>();
        }
    }

    @Test
    public void whenThereAreNoTestsThenNothingIsIgnored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new FileReportUpdater());
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

        TestIgnorer resolver = new MockedTestIgnorer(updater);
        Set<String> result = resolver.getTestsToIgnore(updater.getBaseReport());
        Set<String> expected = new HashSet<>(Arrays.asList("MyTest#testMethod"));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void whenThereIsATestWhichIsModifiedThenIsNotIgnored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList("MyTest.java")),
                new FileReportUpdater());

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
        TestIgnorer resolver = new MockedTestIgnorer(
                new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
                new FileReportUpdater());

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
    public void whenThereAreNestedClassesThenTheParentClassIsUsed() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(
                new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
                new FileReportUpdater());
        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream(
                "[{\n" +
                        "  \"test\": \"MyTest\",\n" +
                        "  \"method\": \"testMethod\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello$NestedClass\"\n" +
                        "  ]\n" +
                        "}]", Charset.forName("UTF-8")));

        Assert.assertEquals(Collections.EMPTY_SET, result);
    }

    @Test
    public void whenThereAreClassesWithSameSuffixCorrectOnesAreIgnored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList(
                "org/walkmod/junit4git/samples/Hello.java")), new FileReportUpdater());

        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream(
                "[{\n" +
                        "  \"test\": \"MyTest\",\n" +
                        "  \"method\": \"testMethod\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello\"\n" +
                        "  ]\n" +
                        "},{" +
                        "  \"test\": \"MyTest2\",\n" +
                        "  \"method\": \"testMethod2\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello2\"\n" +
                        "  ]\n" +
                        "}"+
                        "]", Charset.forName("UTF-8")));

        Assert.assertEquals(new HashSet<>(Arrays.asList("MyTest2#testMethod2")), result);
    }

}

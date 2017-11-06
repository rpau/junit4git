package org.walkmod.junit4git.core.ignorers;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.FileTestReportStorage;
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

        public MockedTestIgnorer(AbstractTestReportStorage updater) {
            this(Collections.EMPTY_SET, updater);
        }

        public MockedTestIgnorer(Set<String> files, AbstractTestReportStorage updater) {
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
    public void when_there_are_no_tests_then_nothing_is_ignored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new FileTestReportStorage());
        Set<String> result = resolver.getTestsToIgnore(IOUtils.toInputStream("[]", Charset.forName("UTF-8")));
        Assert.assertEquals(Collections.EMPTY_SET, result);
    }

    @Test
    public void when_there_is_a_test_which_is_not_modified_then_is_ignored() throws Exception {
        FileTestReportStorage updater = new FileTestReportStorage() {
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
    public void when_there_is_a_test_which_is_modified_then_is_not_ignored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList("MyTest.java")),
                new FileTestReportStorage());

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
    public void when_there_is_a_ref_class_which_is_modified_then_is_not_ignored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(
                new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
                new FileTestReportStorage());

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
    public void when_there_are_nested_classes_then_the_parent_class_is_used() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(
                new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
                new FileTestReportStorage());
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
    public void when_there_are_classes_with_same_suffix_correct_ones_are_ignored() throws Exception {
        TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList(
                "org/walkmod/junit4git/samples/Hello.java")), new FileTestReportStorage());

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

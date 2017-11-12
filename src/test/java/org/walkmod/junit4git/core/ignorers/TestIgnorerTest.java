package org.walkmod.junit4git.core.ignorers;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.FileTestReportStorage;
import org.walkmod.junit4git.core.reports.TestMethodReport;
import org.walkmod.junit4git.javassist.JavassistUtils;
import org.walkmod.junit4git.jgit.GitUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

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
    protected Set<String> getFilesWithUntrackedChanges() throws IOException, GitAPIException {
      return files;
    }

    @Override
    protected Set<String> getChangedAndCommittedFiles() throws IOException, GitAPIException {
      return new HashSet<>();
    }
  }

  @Test
  public void when_there_are_no_tests_then_nothing_is_ignored() throws Exception {
    TestIgnorer resolver = new MockedTestIgnorer(new FileTestReportStorage());
    Set<TestMethodReport> result = resolver.getTestsToIgnore(new TestMethodReport[0]);
    Assert.assertEquals(Collections.EMPTY_SET, result);
  }

  @Test
  public void when_there_is_a_test_which_is_not_modified_then_is_ignored() throws Exception {
    FileTestReportStorage updater = new FileTestReportStorage() {
      @Override
      public TestMethodReport[] getBaseReport() {
        return new Gson().fromJson(
                "[{\n" +
                        "  \"test\": \"MyTest\",\n" +
                        "  \"method\": \"testMethod\",\n" +
                        "  \"classes\": [\n" +
                        "    \"Hello\"\n" +
                        "  ]\n" +
                        "}]", TestMethodReport[].class);
      }
    };

    TestIgnorer resolver = new MockedTestIgnorer(updater);
    Set<TestMethodReport> result = resolver.getTestsToIgnore(updater.getBaseReport());

    Set<String> expected = new HashSet<>(Arrays.asList("MyTest#testMethod"));
    Assert.assertEquals(expected, result.stream()
            .map(TestMethodReport::getTestMethodId)
            .collect(Collectors.toSet()));
  }

  @Test
  public void when_there_is_a_test_which_is_modified_then_is_not_ignored() throws Exception {
    TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList("MyTest.java")),
            new FileTestReportStorage());

    Set<TestMethodReport> result = resolver.getTestsToIgnore(new Gson().fromJson(
            "[{\n" +
                    "  \"test\": \"MyTest\",\n" +
                    "  \"method\": \"testMethod\",\n" +
                    "  \"classes\": [\n" +
                    "    \"Hello\"\n" +
                    "  ]\n" +
                    "}]",TestMethodReport[].class));

    Assert.assertEquals(Collections.EMPTY_SET, result);
  }

  @Test
  public void when_there_is_a_ref_class_which_is_modified_then_is_not_ignored() throws Exception {
    TestIgnorer resolver = new MockedTestIgnorer(
            new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
            new FileTestReportStorage());

    Set<TestMethodReport> result = resolver.getTestsToIgnore(new Gson().fromJson(
            "[{\n" +
                    "  \"test\": \"MyTest\",\n" +
                    "  \"method\": \"testMethod\",\n" +
                    "  \"classes\": [\n" +
                    "    \"Hello\"\n" +
                    "  ]\n" +
                    "}]",TestMethodReport[].class));

    Assert.assertEquals(Collections.EMPTY_SET, result);
  }

  @Test
  public void when_there_are_nested_classes_then_the_parent_class_is_used() throws Exception {
    TestIgnorer resolver = new MockedTestIgnorer(
            new HashSet<>(Arrays.asList("org/walkmod/junit4git/samples/Hello.java")),
            new FileTestReportStorage());
    Set<TestMethodReport> result = resolver.getTestsToIgnore(new Gson().fromJson(
            "[{\n" +
                    "  \"test\": \"MyTest\",\n" +
                    "  \"method\": \"testMethod\",\n" +
                    "  \"classes\": [\n" +
                    "    \"Hello$NestedClass\"\n" +
                    "  ]\n" +
                    "}]",TestMethodReport[].class));

    Assert.assertEquals(Collections.EMPTY_SET, result);
  }

  @Test
  public void when_there_are_classes_with_same_suffix_correct_ones_are_ignored() throws Exception {
    TestIgnorer resolver = new MockedTestIgnorer(new HashSet<>(Arrays.asList(
            "org/walkmod/junit4git/samples/Hello.java")), new FileTestReportStorage());

    Set<TestMethodReport> result = resolver.getTestsToIgnore(new Gson().fromJson(
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
                    "}" +
                    "]",TestMethodReport[].class));

    Assert.assertEquals(new HashSet<>(Arrays.asList("MyTest2#testMethod2")),
            result.stream().map(TestMethodReport::getTestMethodId).collect(Collectors.toSet()));
  }

  @Test
  public void when_there_are_changes_in_master_branch_the_updated_files_are_detected() throws Exception {
    Path path = Files.createTempDirectory("junit-remote-tests");
    GitUtils.buildGitRepoForTest(path.toFile());

    Path localPath = Files.createTempDirectory("junit-local-tests");
    try (Git git = Git.cloneRepository().setURI(path.toUri().toString()).setDirectory(localPath.toFile()).call()) {

      File aux = new File(localPath.toFile(), "test.txt");

      try (FileWriter fw = new FileWriter(aux)) {
        fw.write("test");
        fw.flush();
      }

      git.add().addFilepattern(aux.getName()).call();
      git.commit().setMessage("foo").call();

      TestIgnorer ignorer = new TestIgnorer(localPath.toFile().getCanonicalPath(),
              mock(AbstractTestReportStorage.class), mock(JavassistUtils.class));

      Assert.assertEquals(new HashSet<>(Arrays.asList("test.txt")), ignorer.getChangedAndCommittedFiles());

      FileUtils.deleteDirectory(path.toFile());
      FileUtils.deleteDirectory(localPath.toFile());
    }
  }
}

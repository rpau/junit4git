package org.walkmod.junit4git.core.reports;


import org.eclipse.jgit.api.Git;
import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.jgit.GitRepo;
import org.walkmod.junit4git.jgit.GitRepoBuilder;

import java.io.StringWriter;
import java.util.Collections;

public class GitTestReportStorageTest {

  private static String GIT_NOTES_EXAMPLE = "[\n" +
          "  {\n" +
          "    \"test\": \"MyTest\",\n" +
          "    \"method\": \"testMethod\",\n" +
          "    \"classes\": [\n" +
          "      \"Hello\"\n" +
          "    ]\n" +
          "  }\n" +
          "]";
  @Test
  public void when_it_writes_a_report_then_it_reads_the_same() throws Exception {

    GitRepo repo = GitRepoBuilder.builder().withNotes(GIT_NOTES_EXAMPLE).build();

    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());
    TestMethodReport[] report = updater.getBaseReport();

    Assert.assertEquals(GIT_NOTES_EXAMPLE, updater.gson.toJson(report));

    repo.delete();
  }

  @Test
  public void when_there_are_not_notes_then_it_creates_them() throws Exception {

    GitRepo remote = GitRepoBuilder.builder().build();
    GitRepo local = GitRepoBuilder.clone(remote).build();

    GitTestReportStorage updater = new GitTestReportStorage(local.getPath().toFile().getCanonicalPath());
    updater.prepare();

    Git git = Git.open(local.getPath().toFile());

    Assert.assertNotNull(git.getRepository().findRef(GitTestReportStorage.GIT_NOTES_REF));

    git.close();

    local.delete();
    remote.delete();
  }

  @Test
  public void when_there_are_git_notes_then_it_removes_the_one_from_the_current_commit() throws Exception {

    GitRepo repo = GitRepoBuilder.builder().committing("test.txt", "test")
            .withNotes(GIT_NOTES_EXAMPLE)
            .build();

    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());
    updater.prepare();
    TestMethodReport[] is = updater.getBaseReport();
    Assert.assertNull(is);
    repo.delete();
  }

  @Test
  public void when_the_repo_is_clean_then_it_generates_a_git_notes_writer() throws Exception {
    GitRepo repo = GitRepoBuilder.builder().build();

    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());
    Assert.assertEquals(GitTestReportStorage.GitNotesWriter.class.getName(),
            updater.buildWriter().getClass().getName());

    repo.delete();
  }

  @Test
  public void when_the_repo_has_new_files_then_it_generates_a_dummy_writer() throws Exception {

    GitRepo repo = GitRepoBuilder.builder().modifying("foo.txt", "hello").build();

    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());

    Assert.assertEquals(StringWriter.class.getName(),
            updater.buildWriter().getClass().getName());

    repo.delete();
  }

  @Test
  public void when_it_is_clean_then_writes_the_report() throws Exception {
    GitRepo repo = GitRepoBuilder.builder()
            .committing("FooTest.java", "public FooTest { @org.junit.Test public void test() {} } ")
            .build();
    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());
    Assert.assertFalse(updater.isReportCreated());

    TestMethodReport report = new TestMethodReport("FooTest", "test", Collections.emptySet());
    updater.appendTestReport(report);

    Assert.assertArrayEquals(new TestMethodReport[] {report}, updater.getBaseReport());

    repo.delete();
  }

  @Test
  public void when_it_is_clean_it_appends_to_the_report() throws Exception {
    GitRepo repo = GitRepoBuilder.builder()
            .committing("FooTest.java", "public FooTest { @org.junit.Test public void test() {} } ")
            .committing("BarTest.java", "public BarTest { @org.junit.Test public void test() {} } ")
            .build();
    GitTestReportStorage updater = new GitTestReportStorage(repo.getPath().toFile().getCanonicalPath());
    Assert.assertFalse(updater.isReportCreated());

    TestMethodReport report1 = new TestMethodReport("FooTest", "test", Collections.emptySet());
    TestMethodReport report2 = new TestMethodReport("BarTest", "test", Collections.emptySet());
    updater.appendTestReport(report1);
    updater.appendTestReport(report2);

    Assert.assertArrayEquals(new TestMethodReport[] {report1, report2}, updater.getBaseReport());

    repo.delete();
  }

}

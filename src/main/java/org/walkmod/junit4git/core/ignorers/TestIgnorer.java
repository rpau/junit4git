package org.walkmod.junit4git.core.ignorers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.TestMethodReport;
import org.walkmod.junit4git.javassist.JavassistUtils;
import org.walkmod.junit4git.jgit.JGitUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * It marks with the @Ignore annotations those tests that are not affected by the last changes,
 * which are calculated using the git status command.
 */
public class TestIgnorer {

  private final AbstractTestReportStorage storage;

  private final String executionDir;

  private final JavassistUtils javassist;

  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public TestIgnorer(AbstractTestReportStorage storage) {
    this(".", storage);
  }

  public TestIgnorer(String executionDir, AbstractTestReportStorage storage) {
    this(executionDir, storage, new JavassistUtils());
  }

  public TestIgnorer(Path executionDir, AbstractTestReportStorage storage, JavassistUtils javassist)
          throws IOException {
    this(executionDir.toFile().getCanonicalPath(), storage, javassist);
  }

  public TestIgnorer(String executionDir, AbstractTestReportStorage storage, JavassistUtils javassist) {
    this.storage = storage;
    this.executionDir = executionDir;
    this.javassist = javassist;
  }

  protected Set<TestMethodReport> getTestsToIgnore(TestMethodReport[] report) throws IOException, GitAPIException {
    if (report.length > 0) {
      Set<String> files = getChangedAndCommittedFiles();
      files.addAll(getFilesWithUntrackedChanges());
      return testsToIgnore(files, report);
    } else {
      return new HashSet<>();
    }
  }

  private Set<TestMethodReport> testsToIgnore(Set<String> status, TestMethodReport[] tests) {
    return Stream.of(tests)
            .filter(test -> !test.isImpactedBy(status))
            .collect(Collectors.toSet());
  }

  protected Git open() throws IOException, GitAPIException {
    return Git.open(executionDir());
  }

  protected File executionDir() throws IOException {
    return new File(executionDir).getCanonicalFile();
  }

  /**
   * Returns the list of committed files whose commits are not yet in origin/master
   *
   * @return The list of committed files whose commits are not yet in origin/master
   * @throws IOException
   * @throws GitAPIException
   */
  protected Set<String> getChangedAndCommittedFiles() throws IOException, GitAPIException {
    try (Git git = open()) {
      return new JGitUtils().getUpdatesFromTheBaseBranch(git, "origin/master",
              git.getRepository().getBranch());
    }
  }

  /**
   * Returns the list of existing committed files with pending changes to commit
   *
   * @return the list of existing committed files with pending changes to commit
   * @throws IOException
   * @throws GitAPIException
   */
  protected Set<String> getFilesWithUntrackedChanges() throws IOException, GitAPIException {
    try (Git git = open()) {
      return new JGitUtils().getModifiedOrChangedFiles(git);
    }
  }

  private Map<String, List<TestMethodReport>> testsGroupedByClass() throws Exception {
    return getTestsToIgnore(storage.getBaseReport()).stream()
            .collect(Collectors.groupingBy(TestMethodReport::getTestClass));
  }

  /**
   * Adds the @Ignore annotations to the non affected classes by the last changes
   *
   * @param inst instrumentation class to reload the classes that have been modified
   * @throws Exception in case of modification issues.
   */
  public void ignoreTests(Instrumentation inst) throws Exception {
    javassist.annotateMethods(Ignore.class, inst, testsGroupedByClass());
  }
}

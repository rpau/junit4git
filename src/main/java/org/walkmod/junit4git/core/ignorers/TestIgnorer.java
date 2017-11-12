package org.walkmod.junit4git.core.ignorers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.TestMethodReport;
import org.walkmod.junit4git.javassist.JavassistUtils;
import org.walkmod.junit4git.jgit.JGitUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestIgnorer {

  private final AbstractTestReportStorage updater;

  private final String executionDir;

  private final JavassistUtils javassist;

  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public TestIgnorer(AbstractTestReportStorage updater) {
    this(".", updater);
  }

  public TestIgnorer(String executionDir, AbstractTestReportStorage updater) {
    this(executionDir, updater, new JavassistUtils());
  }

  public TestIgnorer(String executionDir, AbstractTestReportStorage updater, JavassistUtils javassist) {
    this.updater = updater;
    this.executionDir = executionDir;
    this.javassist = javassist;
  }

  protected Set<TestMethodReport> getTestsToIgnore(InputStream is) throws IOException, GitAPIException {

    TestMethodReport[] report = gson.fromJson(new InputStreamReader(is), TestMethodReport[].class);
    if (report.length > 0) {
      Set<String> files = getUpdatesFromTheBaseBranch();
      files.addAll(runGitStatus());
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

  protected Set<String> getUpdatesFromTheBaseBranch() throws IOException, GitAPIException {
    try (Git git = open()) {
      return new JGitUtils().getUpdatesFromTheBaseBranch(git, "origin/master",
              git.getRepository().getBranch());
    }
  }

  protected Set<String> runGitStatus() throws IOException, GitAPIException {
    Set<String> changed = new LinkedHashSet<>();
    Git git = open();
    try {
      Status status = git.status().call();
      changed.addAll(status.getModified());
      changed.addAll(status.getChanged());
    } finally {
      git.close();
    }
    return changed;
  }

  private Map<String, List<TestMethodReport>> testsGroupedByClass() throws Exception {
    return getTestsToIgnore().stream()
            .collect(Collectors.groupingBy(TestMethodReport::getTestClass));
  }

  public void ignoreTests(Instrumentation inst) throws Exception {
    javassist.annotateMethods(Ignore.class, inst, testsGroupedByClass());
  }

  public Set<TestMethodReport> getTestsToIgnore() throws Exception {
    return getTestsToIgnore(updater.getBaseReport());
  }
}

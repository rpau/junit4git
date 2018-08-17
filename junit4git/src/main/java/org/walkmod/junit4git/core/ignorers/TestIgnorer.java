package org.walkmod.junit4git.core.ignorers;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.walkmod.junit4git.core.reports.AbstractTestReportStorage;
import org.walkmod.junit4git.core.reports.TestMethodReport;
import org.walkmod.junit4git.javassist.JavassistUtils;
import org.walkmod.junit4git.jgit.JGitUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.*;
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

  private static Log log = LogFactory.getLog(TestIgnorer.class);

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
    if (report != null && report.length > 0) {
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
    File file = executionDir();
    boolean isGit = new File(file, ".git").exists();
    while (!isGit && file != null) {
      file = file.getParentFile();
      isGit = new File(file, ".git").exists();
    }
    if (isGit) {
      return Git.open(file);
    } else {
      throw new IOException("The execution dir does not belong to a Git repository");
    }
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

  public Map<String, List<TestMethodReport>> testsGroupedByClass() throws Exception {
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
    ClassPool pool = ClassPool.getDefault();
    ignoreTests(inst, pool);
  }

  public void ignoreTests(Instrumentation inst, ClassPool pool) throws Exception {
    Map<String, List<TestMethodReport>> testsToMap = testsGroupedByClass();

    Iterator<String> it = testsToMap.keySet().iterator();
    while (it.hasNext()) {
      String className = it.next();
      ignoreTest(testsToMap, className, Class.forName(className), inst, pool);
    }
  }

  public void ignoreTest(Map<String, List<TestMethodReport>> testsToMap, String className, Class<?> loadedClass,
                          Instrumentation inst, ClassPool pool)  {
    try {
      CtClass clazz = pool.get(className);
      ClassDefinition classDefinition = new ClassDefinition(loadedClass,
              ignoreTest(clazz, testsToMap.get(className)));
      inst.redefineClasses(classDefinition);
      log.info("The test class " + className + " will be ignored");
    } catch (NotFoundException | ClassNotFoundException e) {
      //the class has been removed
    } catch (UnsupportedOperationException e) {
      log.error("Error reloading the class because it was initially loaded");
    } catch (Throwable e) {
      log.error("Error ignoring the test class " + className, e);
    }
  }

  public byte[] ignoreTest(CtClass clazz, List<TestMethodReport> methods) throws Exception {
    String className = clazz.getName();
    clazz.defrost();
    ClassFile ccFile = clazz.getClassFile();
    if (isScalaTest(clazz)) {
      javassist.replaceMethodCallOnConstructors("test", "ignore", clazz, methods);
      log.debug("The scala test class " + className + " has been processed");
    } else {
      ConstPool constpool = ccFile.getConstPool();
      methods.stream()
              .map(TestMethodReport::getTestMethod)
              .forEach(md -> javassist.annotateMethod(Ignore.class, md, clazz, constpool));
    }
    log.debug("The test class " + className + " will be ignored");
    return clazz.toBytecode();
  }

  public byte[] ignoreTest(String className, List<TestMethodReport> methods) throws Exception {

    ClassPool pool = ClassPool.getDefault();
    CtClass clazz = pool.get(className);
    return ignoreTest(clazz, methods);
  }

  public boolean isScalaTest(CtClass clazz) {
    try {
      CtClass superClazz = clazz.getSuperclass();
      while (superClazz != null
              && !superClazz.getName().equals("org.scalatest.FunSuite")
              && !superClazz.getName().equals("java.lang.Object")) {
        superClazz = superClazz.getSuperclass();
      }
      return superClazz != null && superClazz.getName().equals("org.scalatest.FunSuite");
    } catch (Exception e) {
      return false;
    }
  }
}

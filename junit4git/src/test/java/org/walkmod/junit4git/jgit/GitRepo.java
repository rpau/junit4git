package org.walkmod.junit4git.jgit;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.walkmod.junit4git.core.reports.GitTestReportStorage;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GitRepo {

  private final Path path;
  private String branch = "master";
  private final Map<String, String> committedFiles;
  private final Map<String, String> modifiedFiles;

  private String notes;

  protected GitRepo() {
    this.path = null;
    this.committedFiles = new HashMap<>();
    this.modifiedFiles = new HashMap<>();
  }

  protected GitRepo(Path path){
    this.path = path;
    this.committedFiles = new HashMap<>();
    this.modifiedFiles = new HashMap<>();
  }

  protected void setNotes(String notes) {
    this.notes = notes;
  }

  protected void setBranch(String branch) {
    this.branch = branch;
  }

  public Path getPath() {
    return path;
  }

  public Map<String, String> getCommittedFiles() {
    return committedFiles;
  }

  public Map<String, String> getModifiedFiles() {
    return modifiedFiles;
  }

  private void writeFile(String file, String contents) {
    try {
      File existingFile = new File(path.toFile(), file);
      try (FileWriter fw = new FileWriter(existingFile)) {
        fw.write(contents);
        fw.flush();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addFile(Git git, String file) {
    try {
      File existingFile = new File(path.toFile(), file);
      git.add().addFilepattern(existingFile.getName()).call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void init() {
    if (path != null) {
      try (Git parentGit = GitUtils.buildGitRepoForTest(path.toFile())) {


        if (!branch.equals("master")) {
          parentGit.checkout().setCreateBranch(true).setName(branch).call();
        }

        committedFiles.keySet().stream().forEach(file -> {
              writeFile(file, committedFiles.get(file));
              addFile(parentGit, file);
          }
        );

        parentGit.commit().setMessage("init commit").call();

        modifiedFiles.keySet().stream().forEach(file -> {
          writeFile(file, modifiedFiles.get(file));
        });

        if (notes != null) {
          GitTestReportStorage updater = new GitTestReportStorage(path.toFile().getCanonicalPath());
          updater.createGitNotesRef(parentGit);

          GitTestReportStorage.GitNotesWriter writer = new GitTestReportStorage.GitNotesWriter(updater);
          writer.write(notes);
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void delete() {
    FileUtils.deleteQuietly(path.toFile());
  }
}

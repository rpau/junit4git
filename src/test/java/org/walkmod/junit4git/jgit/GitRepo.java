package org.walkmod.junit4git.jgit;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class GitRepo {

  private final Path path;
  private final Map<String, String> committedFiles;
  private final Map<String, String> modifiedFiles;


  public GitRepo() {
    this.path = null;
    this.committedFiles = new HashMap<>();
    this.modifiedFiles = new HashMap<>();
  }

  public GitRepo(Path path){
    this.path = path;
    this.committedFiles = new HashMap<>();
    this.modifiedFiles = new HashMap<>();
  }

  public GitRepo(GitRepo repo) {
    this.path = repo.path;
    this.committedFiles = repo.committedFiles;
    this.modifiedFiles = repo.modifiedFiles;
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
        committedFiles.keySet().stream().forEach(file -> {
              writeFile(file, committedFiles.get(file));
              addFile(parentGit, file);
          }
        );
        parentGit.commit().setMessage("init commit").call();

        modifiedFiles.keySet().stream().forEach(file -> {
          writeFile(file, modifiedFiles.get(file));
        });

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void delete() throws IOException {
    FileUtils.deleteDirectory(path.toFile());
  }
}

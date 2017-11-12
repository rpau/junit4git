package org.walkmod.junit4git.jgit;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitRepoBuilder {

  private GitRepo repo = new GitRepo();

  private GitRepoBuilder(GitRepo repo) {
    this.repo = repo;
  }

  public static GitRepoBuilder builder() throws IOException {
    return new GitRepoBuilder(new GitRepo(Files.createTempDirectory("junit-remote-tests")));
  }

  public static GitRepoBuilder clone(GitRepo repo) throws IOException, GitAPIException {
    Path localPath = Files.createTempDirectory("junit-local-tests");
    Git.cloneRepository().setURI(repo.getPath().toUri().toString()).setDirectory(localPath.toFile()).call();
    return new GitRepoBuilder(new GitRepo(localPath));
  }

  public GitRepoBuilder committing(String file, String contents) {
    repo.getCommittedFiles().put(file, contents);
    return this;
  }

  public GitRepoBuilder modifying(String file, String contents) {
    repo.getModifiedFiles().put(file, contents);
    return this;
  }

  public GitRepo build() {
     repo.init();
     return repo;
  }

}

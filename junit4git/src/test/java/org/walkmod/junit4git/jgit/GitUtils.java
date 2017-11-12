package org.walkmod.junit4git.jgit;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.File;
import java.io.IOException;

public class GitUtils {

  public static void setRemote(String url, Git git) throws IOException {
    StoredConfig config = git.getRepository().getConfig();
    config.setString("remote", "origin", "url", url);
    config.save();
  }

  public static void emptyCommit(Git git, String message) throws IOException, GitAPIException {
    git.commit().setAllowEmpty(true).setMessage(message).call();
  }

  public static Git buildGitRepoForTest(File directory) throws IOException, GitAPIException {
    Git git = Git.init().setDirectory(directory).call();
    GitUtils.setRemote(directory.toURL().toString(), git);
    GitUtils.emptyCommit(git, "");
    return git;
  }
}

package org.walkmod.junit4git.jgit;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JGitUtils {

  public Set<String> getUpdatesFromTheBaseBranch(Git git, String baseRef, String headRef) throws IOException, GitAPIException {

    Set<String> files = new LinkedHashSet<>();
    Ref baseBranch = git.getRepository().findRef(baseRef);
    Ref headBranch = git.getRepository().findRef(headRef);
    RevWalk walk = new RevWalk(git.getRepository());
    RevCommit baseCommit = walk.parseCommit(baseBranch.getObjectId());
    RevCommit headCommit = walk.parseCommit(headBranch.getObjectId());

    try (ObjectReader reader = git.getRepository().newObjectReader()) {
      CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
      oldTreeIter.reset(reader, baseCommit.getTree());
      CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
      newTreeIter.reset(reader, headCommit.getTree());

      List<DiffEntry> diffs= git.diff()
              .setNewTree(newTreeIter)
              .setOldTree(oldTreeIter)
              .call();
      for (DiffEntry entry : diffs) {
        files.add(entry.getNewPath());
      }
    }
    return files;
  }
}

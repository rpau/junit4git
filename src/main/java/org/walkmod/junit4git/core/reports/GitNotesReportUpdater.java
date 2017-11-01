package org.walkmod.junit4git.core.reports;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class GitNotesReportUpdater extends AbstractReportUpdater {

    private static String BASE_BRANCH = "master";

    private static String GIT_NOTES_REF = "refs/notes/tests";

    private final String executionDir;

    public GitNotesReportUpdater(){
        this(".");
    }

    public GitNotesReportUpdater(String executionDir) {
        this.executionDir = executionDir;
    }

    public void removeContents() {
        try {
            Git git = open();
            Ref ref = fetchNotesRef(git);
            if (ref == null) {
                createGitNotesRef(git);
            }
            if (isInMasterAndClean(git)) {
                removeLastNote(git);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error removing the existing Git notes in " + GIT_NOTES_REF, e);
        }
    }

    public Ref fetchNotesRef(Git git) throws IOException, GitAPIException{
        Ref ref = git.getRepository().findRef(GIT_NOTES_REF);
        boolean isInRemote = git.lsRemote().call().stream()
                    .filter(remoteRef -> remoteRef.getName().equals(GIT_NOTES_REF))
                    .findFirst()
                    .isPresent();

        if (isInRemote) {
            git.fetch().setRemote("origin")
                    .setDryRun(false)
                    .setRefSpecs(new RefSpec(GIT_NOTES_REF + ":" + GIT_NOTES_REF))
                    .call();
        }
        return ref;
    }

    private void removeLastNote(Git git) throws IOException, GitAPIException {
        RevWalk walk = new RevWalk(git.getRepository());
        RevCommit commit = walk.parseCommit(getOriginHead(git).getObjectId());
        git.notesRemove().setNotesRef(GIT_NOTES_REF)
                .setObjectId(commit).call();
    }

    private void createGitNotesRef(Git git) throws IOException {
        RefUpdate ru = git.getRepository().getRefDatabase().newUpdate(GIT_NOTES_REF, true);
        ru.setNewObjectId(getOriginHead(git).getObjectId());
        ru.update();
    }

    @Override
    public InputStream getBaseReport() throws IOException {
        String notes = getNotes();
        return new ByteArrayInputStream(notes.getBytes());
    }

    private Ref getOriginHead(Git git) throws IOException {
        return git.getRepository().findRef("origin/" + BASE_BRANCH);
    }

    protected Git open() throws IOException  {
        return Git.open(new File(executionDir).getCanonicalFile());
    }

    @Override
    protected Reader buildReader() throws IOException {
        return new InputStreamReader(new ByteArrayInputStream(getNotes().getBytes()));
    }

    private String getNotes() throws IOException {
        Git git = open();

        try {
            fetchNotesRef(git);

            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit commit = walk.parseCommit(getOriginHead(git).getObjectId());
            Note note = git.notesShow().setNotesRef(GIT_NOTES_REF)
                    .setObjectId(commit).call();

            if (note != null) {
                return new String(git.getRepository().open(note.getData()).getCachedBytes(),
                        StandardCharsets.UTF_8);
            } else {
                return "";
            }

        } catch(GitAPIException e) {
            throw new IOException("Error reading Git notes", e);
        } finally {
            git.close();
        }
    }

    @Override
    protected Writer buildWriter() throws IOException {
        Git git = open();
        try {
            if (isInMasterAndClean(git)) {
                return new GitNotesWriter(this);
            } else {
                return new StringWriter();
            }
        } catch (GitAPIException e) {
            throw new IOException("Error building the git notes writer", e);
        } finally {
            git.close();
        }
    }

    private boolean isInMasterAndClean(Git git) throws GitAPIException, IOException {
        if (git.getRepository().getBranch().equals(BASE_BRANCH)) {

            Ref baseBranch = git.getRepository().findRef("origin/master");
            Ref headBranch = git.getRepository().findRef(git.getRepository().getBranch());

            RevWalk walk = new RevWalk(git.getRepository());

            RevCommit baseCommit = walk.parseCommit(baseBranch.getObjectId());
            RevCommit headCommit = walk.parseCommit(headBranch.getObjectId());
            return baseCommit.equals(headCommit);
        }
        return false;
    }


    private static class GitNotesWriter extends StringWriter {

        private final GitNotesReportUpdater updater;

        public GitNotesWriter(GitNotesReportUpdater updater) {
            this.updater = updater;
        }

        @Override
        public void write(String str){
            try {
                Git git = updater.open();
                try {
                    RevWalk walk = new RevWalk(git.getRepository());
                    RevCommit commit = walk.parseCommit(updater.getOriginHead(git).getObjectId());
                    git.notesAdd().setNotesRef(GIT_NOTES_REF)
                            .setObjectId(commit)
                            .setMessage(str).call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                } finally {
                    git.close();
                }
            } catch (Exception e){
                throw new RuntimeException("Error from the GitNotesWriter", e);
            }
        }
    }

    @Override
    protected boolean isReportCreated() throws IOException {
        return !StringUtils.isEmptyOrNull(getNotes());
    }
}

package org.walkmod.junit4git.core.reports;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class GitNotesReportUpdater extends AbstractReportUpdater {

    private static String BASE_BRANCH = "master";

    private static String GIT_NOTES_REF = "refs/notes/tests";

    public void removeContents() {
        try {
            Git git = open();
            Ref ref = git.getRepository().findRef(GIT_NOTES_REF);
            if (ref == null) {
                createGitNotesRef(git);
                if (isInMasterAndClean(git)) {
                    removeLastNote(git);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error removing the existing Git notes in " + GIT_NOTES_REF, e);
        }
    }

    private void removeLastNote(Git git) throws IOException, GitAPIException {
        RevWalk walk = new RevWalk(git.getRepository());
        RevCommit commit = walk.parseCommit(getHead(git).getObjectId());
        git.notesRemove().setNotesRef(GIT_NOTES_REF)
                .setObjectId(commit).call();
    }

    private void createGitNotesRef(Git git) throws IOException {
        RefUpdate ru = git.getRepository().getRefDatabase().newUpdate(GIT_NOTES_REF, true);
        ru.setNewObjectId(getHead(git).getObjectId());
        ru.update();
    }

    @Override
    public InputStream getBaseReport() throws IOException {
        return new ByteArrayInputStream(getNotes().getBytes());
    }

    private Ref getHead(Git git) throws IOException {
        return git.getRepository().findRef("refs/heads/" + BASE_BRANCH);
    }

    private  Git open() throws IOException  {
        return Git.open(new File(".").getCanonicalFile());
    }

    @Override
    protected Reader buildReader() throws IOException {
        return new InputStreamReader(new ByteArrayInputStream(getNotes().getBytes()));
    }

    private String getNotes() throws IOException {
        Git git = open();

        try {
            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit commit = walk.parseCommit(getHead(git).getObjectId());
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
            Status status = git.status().call();
            if (status.getChanged().isEmpty() && status.getModified().isEmpty()) {
                return !(status.getAdded().stream()
                        .filter(file -> file.endsWith(".java"))
                        .findFirst()
                        .isPresent());
            }
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
                    RevCommit commit = walk.parseCommit(updater.getHead(git).getObjectId());
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

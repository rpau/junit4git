package org.walkmod.junit4git.core.reports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class GitTestReportStorage extends AbstractTestReportStorage {

    private static String BASE_BRANCH = "master";

    protected static String GIT_NOTES_REF = "refs/notes/tests";

    private final String executionDir;

    private static Log log = LogFactory.getLog(GitTestReportStorage.class);

    public GitTestReportStorage(){
        this(".");
    }

    public GitTestReportStorage(String executionDir) {
        this.executionDir = executionDir;
    }

    public void prepare() {
        try (Git git = open()) {
            Ref ref = fetchNotesRef(git);
            if (ref == null) {
                createGitNotesRef(git);
            } else if (isClean(git)) {
                removeLastNote(git);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error removing the existing Git notes in " + GIT_NOTES_REF, e);
        }
    }

    public Ref fetchNotesRef(Git git) throws IOException, GitAPIException {
        Ref ref = git.getRepository().findRef(GIT_NOTES_REF);
        if (areNotesInRemote(git)) {
            git.fetch().setRemote("origin")
                    .setDryRun(false)
                    .setRefSpecs(new RefSpec(GIT_NOTES_REF + ":" + GIT_NOTES_REF))
                    .call();
        }
        return ref;
    }

    protected boolean areNotesInRemote(Git git) throws IOException, GitAPIException {
        return git.lsRemote().call().stream()
                .filter(remoteRef -> remoteRef.getName().equals(GIT_NOTES_REF))
                .findFirst()
                .isPresent();
    }

    private void removeLastNote(Git git) throws IOException, GitAPIException {
        RevWalk walk = new RevWalk(git.getRepository());
        RevCommit commit = walk.parseCommit(getBaseObjectId(git));
        git.notesRemove().setNotesRef(GIT_NOTES_REF)
                .setObjectId(commit).call();
    }

    protected void createGitNotesRef(Git git) throws IOException {
        RefUpdate ru = git.getRepository().getRefDatabase().newUpdate(GIT_NOTES_REF, true);
        ru.setNewObjectId(getBaseObjectId(git));
        ru.update();
    }

    private ObjectId getBaseObjectId(Git git) throws IOException {
        return getOriginHead(git).orElse(getHead(git)).getObjectId();
    }

    @Override
    public InputStream getBaseReport() throws IOException {
        String notes = getNotes();
        return new ByteArrayInputStream(notes.getBytes());
    }

    private Optional<Ref> getOriginHead(Git git) throws IOException {
        return Optional.ofNullable(git.getRepository().findRef("origin/" + BASE_BRANCH));
    }

    private Ref getHead(Git git) throws IOException {
        return git.getRepository().findRef(git.getRepository().getBranch());
    }

    protected Git open() throws IOException  {
        return Git.open(new File(executionDir).getCanonicalFile());
    }

    @Override
    protected Reader buildReader() throws IOException {
        return new InputStreamReader(new ByteArrayInputStream(getNotes().getBytes()));
    }

    protected String getNotes() throws IOException {
        try (Git git = open()){
            fetchNotesRef(git);

            RevWalk walk = new RevWalk(git.getRepository());
            RevCommit commit = walk.parseCommit(getBaseObjectId(git));
            Note note = git.notesShow().setNotesRef(GIT_NOTES_REF)
                    .setObjectId(commit).call();

            if (note != null) {
                log.debug(String.format("Git Notes found at %s for the commit %s", GIT_NOTES_REF,
                        commit.getName()));
                return new String(git.getRepository().open(note.getData()).getCachedBytes(),
                        StandardCharsets.UTF_8);
            } else {
                log.debug(String.format("Ops! Git Notes are not found at %s for the commit %s", GIT_NOTES_REF,
                        commit.getName()));
                return "";
            }

        } catch(GitAPIException e) {
            log.error("Error reading Git notes", e);
            throw new IOException("Error reading Git notes", e);
        }
    }

    @Override
    protected Writer buildWriter() throws IOException {

        try(Git git = open()) {
            if (isClean(git)) {
                log.info("Tests Report:[READY]. Master branch is clean");
                return new GitNotesWriter(this);
            } else {
                log.info("Tests Report [OMITTED]. If you are in master branch, " +
                        "check that there are not pending changes to commit");
                return new StringWriter();
            }
        } catch (GitAPIException e) {
            log.error("Error checking master branch status", e);
            throw new IOException("Error building the git notes writer", e);
        }
    }

    protected boolean isClean(Git git) throws GitAPIException, IOException {
        if (git.getRepository().getBranch().equals(BASE_BRANCH)) {

            Optional<Ref> baseBranch = getOriginHead(git);
            if (baseBranch.isPresent()) {
                RevWalk walk = new RevWalk(git.getRepository());
                RevCommit baseCommit = walk.parseCommit(getBaseObjectId(git));
                RevCommit headCommit = walk.parseCommit(getHead(git).getObjectId());

                log.info(String.format("origin/master Commit: [%s], branch: [%s]",
                        baseCommit.getName(), headCommit.getName()));
                return baseCommit.equals(headCommit) && git.status().call().isClean();
            } else {
                // there is no origin
                return git.status().call().isClean();
            }
        }
        return false;
    }


    public static class GitNotesWriter extends StringWriter {

        private final GitTestReportStorage updater;

        public GitNotesWriter(GitTestReportStorage updater) {
            this.updater = updater;
        }

        @Override
        public void write(String str){
            try(Git git = updater.open()) {
                RevWalk walk = new RevWalk(git.getRepository());
                RevCommit commit = walk.parseCommit(updater.getBaseObjectId(git));
                git.notesAdd().setNotesRef(GIT_NOTES_REF)
                        .setObjectId(commit)
                        .setMessage(str).call();

            } catch (Exception e){
                log.error("Error writing Tests Report in the Git Notes", e);
                throw new RuntimeException("Error from the GitNotesWriter", e);
            }
        }
    }

    @Override
    protected boolean isReportCreated() throws IOException {
        return !StringUtils.isEmptyOrNull(getNotes());
    }
}

package rpau.smartesting.core;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.StringUtils;

import java.io.*;

public class GitNotesReportUpdater extends AbstractReportUpdater {


    public void removeContents() {
        try {
            Git git = open();
            Ref ref = git.getRepository().findRef(getRef());
            if (ref == null) {
                RefUpdate ru = git.getRepository().getRefDatabase().newUpdate(getRef(), true);
                ru.setNewObjectId(getHead(git).getObjectId());
                ru.update();
                try {
                    RevWalk walk = new RevWalk(git.getRepository());
                    RevCommit commit = walk.parseCommit(getHead(git).getObjectId());
                    git.notesRemove().setNotesRef(getRef())
                            .setObjectId(commit).call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                } finally {
                    git.close();
                }
            }

        } catch (Exception e) {

        }
    }

    @Override
    public InputStream getBaseReport() throws IOException {
        return new ByteArrayInputStream(getNotes().getBytes());
    }

    private Ref getHead(Git git) throws IOException {
        return git.getRepository().findRef("refs/heads/master");
    }

    private  Git open() throws IOException  {
        return Git.open(new File(".").getCanonicalFile());
    }

    private  String getRef() {
        return "refs/notes/tests";
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
            Note note = git.notesShow().setNotesRef(getRef())
                    .setObjectId(commit).call();

            if (note != null) {
                return new String(git.getRepository().open(note.getData()).getCachedBytes(),
                        "UTF-8");
            } else {
                return "";
            }

        } catch(GitAPIException e) {
            throw new IOException("Error reading notes", e);
        } finally {
            git.close();
        }
    }

    @Override
    protected Writer buildWriter() throws IOException {
        Git git = open();
        try {
            if (git.getRepository().getBranch().equals("master")) {
                if (git.status().call().isClean()) {
                    return new GitNotesWriter(this);
                } else {
                    return new StringWriter();
                }
            } else {
                return new StringWriter();
            }
        } catch (GitAPIException e) {
            throw new IOException("Error writing Git notes", e);
        } finally {
            git.close();
        }
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
                    git.notesAdd().setNotesRef(updater.getRef())
                            .setObjectId(commit)
                            .setMessage(str).call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                } finally {
                    git.close();
                }
            } catch (Exception e){}
        }
    }

    @Override
    protected boolean isReportCreated() throws IOException {
        return !StringUtils.isEmptyOrNull(getNotes());
    }
}

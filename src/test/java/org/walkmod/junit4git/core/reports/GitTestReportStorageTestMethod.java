package org.walkmod.junit4git.core.reports;


import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.walkmod.junit4git.jgit.GitUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class GitTestReportStorageTestMethod {

    private Git git;

    private Path path;

    private static String GIT_NOTES_EXAMPLE = "[{\n" +
            "  \"test\": \"MyTest\",\n" +
            "  \"method\": \"testMethod\",\n" +
            "  \"classes\": [\n" +
            "    \"Hello\"\n" +
            "  ]\n" +
            "}]";

    @Before
    public void prepare() throws Exception {
        path = Files.createTempDirectory("junit-tests");
        git = GitUtils.buildGitRepoForTest(path.toFile());
    }

    @After
    public void clean() throws Exception {
        git.close();
        FileUtils.deleteDirectory(path.toFile());
    }

    @Test
    public void when_it_writes_a_report_then_it_reads_the_same() throws Exception {
        GitTestReportStorage updater = new GitTestReportStorage();
        updater.createGitNotesRef(git);

        GitTestReportStorage.GitNotesWriter writer = new GitTestReportStorage.GitNotesWriter(updater);
        writer.write(GIT_NOTES_EXAMPLE);
        InputStream is = updater.getBaseReport();
        String result = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));

        Assert.assertEquals(GIT_NOTES_EXAMPLE, result);
    }

    @Test
    public void when_there_are_not_notes_then_it_creates_them() throws Exception {
        GitTestReportStorage updater = new GitTestReportStorage(path.toFile().getCanonicalPath()) {
            @Override
            public boolean areNotesInRemote(Git git) {
                return false;
            }
        };
        updater.prepare();
        Assert.assertNotNull(git.getRepository().findRef(GitTestReportStorage.GIT_NOTES_REF));
    }

    @Test
    public void when_there_are_git_notes_then_it_removes_the_one_from_the_current_commit() throws Exception {
        GitTestReportStorage updater = new GitTestReportStorage(path.toFile().getCanonicalPath()) {
            @Override
            public Ref fetchNotesRef(Git git) throws IOException, GitAPIException {
                return git.getRepository().findRef(GitTestReportStorage.GIT_NOTES_REF);
            }
        };
        updater.createGitNotesRef(git);
        GitTestReportStorage.GitNotesWriter writer = new GitTestReportStorage.GitNotesWriter(updater);
        writer.write(GIT_NOTES_EXAMPLE);
        updater.prepare();

        InputStream is = updater.getBaseReport();
        String result = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
        Assert.assertEquals("", result);
    }

    @Test
    public void when_the_repo_is_clean_then_it_generates_a_git_notes_writer() throws Exception {
        GitTestReportStorage updater = new GitTestReportStorage(path.toFile().getCanonicalPath());
        Assert.assertEquals(GitTestReportStorage.GitNotesWriter.class.getName(),
                updater.buildWriter().getClass().getName());
    }

    @Test
    public void when_the_repo_has_new_files_then_it_generates_a_dummy_writer() throws Exception {
        GitTestReportStorage updater = new GitTestReportStorage(path.toFile().getCanonicalPath());
        File file = new File(path.toFile(),"foo.txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("hello");
        }
        Assert.assertEquals(StringWriter.class.getName(),
                updater.buildWriter().getClass().getName());
    }
}

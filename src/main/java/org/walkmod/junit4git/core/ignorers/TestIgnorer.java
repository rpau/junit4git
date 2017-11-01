package org.walkmod.junit4git.core.ignorers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Ignore;
import org.walkmod.junit4git.core.reports.AbstractReportUpdater;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestIgnorer {

    private final AbstractReportUpdater updater;

    private final String executionDir;

    public TestIgnorer(AbstractReportUpdater updater) {
        this(".", updater);
    }

    public TestIgnorer(String executionDir, AbstractReportUpdater updater) {
        this.updater = updater;
        this.executionDir = executionDir;
    }

    protected Set<String> getTestsToIgnore(InputStream is) throws IOException, GitAPIException {
        String result = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));

        JsonElement element = new JsonParser().parse(result);
        if (element.isJsonArray()) {
            JsonArray tests = element.getAsJsonArray();
            Set<String> files = getUpdatesFromTheBaseBranch();
            files.addAll(runGitStatus());
            return testsToIgnore(files, tests);
        } else {
            return new HashSet<>();
        }
    }

    private boolean isModified(String className, Set<String> status) {
        Iterator<String> it = status.iterator();
        boolean found = false;
        while(it.hasNext() && !found) {
            String statusFile = it.next();
            if (!statusFile.endsWith(".class")) {
                found = matchesWithFile(className, statusFile);
            }
        }
        return found;
    }

    protected boolean matchesWithFile(String className, String statusFile) {
        String testFilePath = toFilePath(getParentClassName(className));
        return fileWithoutExtension(statusFile).endsWith(testFilePath);
    }

    private String fileWithoutExtension(String statusFile) {
        int index = statusFile.lastIndexOf(".");
        if (index > -1) {
            return statusFile.substring(0, index);
        }
        return statusFile;
    }

    private String getParentClassName(String className) {
        int innerClassIndex = className.indexOf("$");
        if (innerClassIndex > -1) {
            return className.substring(0, innerClassIndex);
        }
        return className;
    }

    private String toFilePath(String name) {
        return name.replaceAll("\\.", "/");
    }

    private Set<String> testsToIgnore(Set<String> status, JsonArray tests) {
        Set<String> testsToIgnore = new HashSet<>();
        Iterator<JsonElement> it = tests.iterator();
        while(it.hasNext()) {
            JsonObject json = it.next().getAsJsonObject();
            String testClass = json.get("test").getAsString();
            String testMethod = json.get("method").getAsString();
            if (!isModified(testClass, status)) {
                JsonArray classes = json.get("classes").getAsJsonArray();
                Iterator<JsonElement> itClasses = classes.iterator();
                boolean found = false;
                while (itClasses.hasNext() && !found) {
                    found = isModified(itClasses.next().getAsString(), status);
                }
                if (!found) {
                    testsToIgnore.add(testClass + "#" + testMethod);
                }
            }
        }
        return testsToIgnore;
    }

    protected Git open() throws IOException, GitAPIException {
        return Git.open(executionDir());
    }

    protected File executionDir() throws IOException {
        return new File(executionDir).getCanonicalFile();
    }

    protected Set<String> getUpdatesFromTheBaseBranch() throws IOException, GitAPIException {
        Git git = open();
        Set<String> files = new LinkedHashSet<>();
        String currentBranch = git.getRepository().getBranch();
        Ref baseBranch = git.getRepository().findRef("origin/master");
        Ref headBranch = git.getRepository().findRef(currentBranch);
        RevWalk walk = new RevWalk(git.getRepository());
        RevCommit baseCommit = walk.parseCommit(baseBranch.getObjectId());
        RevCommit headCommit = walk.parseCommit(headBranch.getObjectId());

        try {

            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, baseCommit.getTree());
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, headCommit.getTree());

                // finally get the list of changed files

                List<DiffEntry> diffs= git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();
                for (DiffEntry entry : diffs) {
                    files.add(entry.getNewPath());
                }

            }
          /*  Iterable<RevCommit> res = git.log().addRange(baseBranch.getObjectId(), headBranch.getObjectId()).call();
            Iterator<RevCommit> it = res.iterator();
            while (it.hasNext()) {
                files.addAll(filesFromACommit(git, it.next()));
            }*/
        } finally {
            git.close();
        }
        return files;
    }

    private Set<String> filesFromACommit(Git git, RevCommit commit)throws IOException, GitAPIException{
        Set<String> files = new LinkedHashSet<>();
        TreeWalk treeWalk = new TreeWalk(git.getRepository());
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);
        while (treeWalk.next()) {
            File file = new File(executionDir(), treeWalk.getPathString());
            if (!file.isDirectory()) {
                files.add(file.getPath());
            }
        }
        treeWalk.close();
        return files;
    }

    protected Set<String> runGitStatus() throws IOException, GitAPIException {
        Set<String> changed = new LinkedHashSet<>();
        Git git = open();
        try {
            Status status = git.status().call();
            changed.addAll(status.getModified());
            changed.addAll(status.getChanged());
        } finally {
            git.close();
        }
        return changed;
    }

    private Map<String, List<String>> testsByClass() throws Exception{
        Set<String> tests = getTestsToIgnore();
        Iterator<String> it = tests.iterator();
        Map<String, List<String>> result = new HashMap<>();
        while(it.hasNext()) {
            String next = it.next();
            String[] parts = next.split("#");
            List<String> aux = result.get(parts[0]);
            if (aux == null) {
                aux = new LinkedList<>();
                result.put(parts[0], aux);
            }
            aux.add(parts[1]);
        }
        return result;
    }

    public void ignoreTests(Instrumentation inst) throws Exception {
        ClassPool pool = ClassPool.getDefault();

        Map<String, List<String>> testsToMap = testsByClass();

        Iterator<String> it = testsToMap.keySet().iterator();

        while (it.hasNext()) {
            String className = it.next();
            try {
                CtClass clazz = pool.get(className);
                ClassFile ccFile = clazz.getClassFile();
                ConstPool constpool = ccFile.getConstPool();
                List<String> methods = testsToMap.get(className);
                methods.forEach(md -> {
                    try {
                        CtMethod method = clazz.getDeclaredMethod(md);

                        AnnotationsAttribute attr = (AnnotationsAttribute) method.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);

                        attr.addAnnotation(new Annotation(Ignore.class.getName(), constpool));
                        method.getMethodInfo().addAttribute(attr);
                    } catch (NotFoundException e) {
                        //the method has been removed
                    } catch (Exception e) {
                        throw new RuntimeException("Error adding @Ignore annotations ", e);
                    }
                });

                inst.redefineClasses(new ClassDefinition(Class.forName(className), clazz.toBytecode()));
            } catch (NotFoundException e) {
                //the class has been removed
            }

        }
    }

    public  Set<String> getTestsToIgnore() throws Exception {
        return getTestsToIgnore(updater.getBaseReport());
    }
}

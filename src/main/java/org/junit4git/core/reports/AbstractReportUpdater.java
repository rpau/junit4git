package org.junit4git.core.reports;


import com.google.gson.*;
import org.junit4git.core.AgentClassTransformer;

import java.io.*;
import java.util.Set;

public abstract class AbstractReportUpdater {

    protected Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void removeContents() {
        try (Writer writer = buildWriter()){
            writer.write("");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract InputStream getBaseReport() throws IOException;

    protected abstract Reader buildReader() throws IOException;

    protected abstract Writer buildWriter() throws IOException;

    protected abstract boolean isReportCreated() throws IOException;

    protected JsonArray readReport() {
        JsonArray tests = new JsonArray();
        try {
            if (isReportCreated()) {
                JsonElement element = new JsonParser().parse(buildReader());
                if (element.isJsonArray()) {
                    tests = element.getAsJsonArray();
                }

            }
        } catch (Exception e) {}
        return tests;
    }

    private void appendTestResult(JsonObject object, JsonArray tests) {
        tests.add(object);
        try (Writer writer = buildWriter()){
            writer.write(gson.toJson(tests));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonObject toTestResult(String testClass, String testMethod, Set<String> referencedClasses) {
        JsonObject object = new JsonObject();
        object.addProperty("test", testClass);
        object.addProperty("method", testMethod);
        object.add("classes", toJsonArray(referencedClasses));
        return object;
    }

    private JsonArray toJsonArray(Set<String> array) {
        JsonArray res = new JsonArray();
        array.forEach(value -> res.add(value));
        return res;
    }

    protected Set<String> getReferencedClasses() {
        return AgentClassTransformer.destroyContext();
    }

    protected void onEnd(String event, String testClass, String testMethod, Set<String> referencedClasses) {

        appendTestResult(toTestResult(testClass, testMethod, referencedClasses), readReport());
    }

    protected void onStart(String event, String testClass, String testMethod) {
        AgentClassTransformer.createContext();
    }

    public void update(String event, String testClass, String testMethod){
        Set<String> referencedClasses = getReferencedClasses();
        if ("start".equals(event)) {
            onStart(event, testClass, testMethod);
        } else {
            onEnd(event, testClass, testMethod, referencedClasses);
        }
    }
}

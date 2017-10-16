package rpau.smartesting.core;


import com.google.gson.*;

import java.io.*;
import java.util.Set;

public class ReportUpdater {

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private File report = new File("smart-testing-report.json");

    public void clearFile() {
        try (Writer writer = buildWriter()){
            writer.write("");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Writer buildWriter() throws IOException {
        return new FileWriter(report);
    }

    protected JsonArray readReport() {
        JsonArray tests = new JsonArray();
        if (report.exists()) {
            try {
                tests = new JsonParser().parse(
                        new InputStreamReader(new FileInputStream(report)))
                        .getAsJsonArray();
            } catch (Exception e) {}
        }
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
        return LoggerClassTransformer.destroyContext();
    }

    protected void onEnd(String event, String testClass, String testMethod, Set<String> referencedClasses) {

        appendTestResult(toTestResult(testClass, testMethod, referencedClasses), readReport());
    }

    protected void onStart(String event, String testClass, String testMethod) {
        LoggerClassTransformer.createContext();
    }

    public void update(String event, String testClass, String testMethod){
        Set<String> referencedClasses = getReferencedClasses();
        System.out.println("event "+event+" testC "+testClass + "testM "+testMethod);
        if ("start".equals(event)) {
            onStart(event, testClass, testMethod);
        } else {
            onEnd(event, testClass, testMethod, referencedClasses);
        }
    }
}

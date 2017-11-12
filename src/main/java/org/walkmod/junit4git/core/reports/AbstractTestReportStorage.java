package org.walkmod.junit4git.core.reports;


import com.google.gson.*;

import java.io.*;
import java.util.Set;

public abstract class AbstractTestReportStorage {

    protected Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void prepare() {
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

    private JsonArray toJsonArray(Set<String> array) {
        JsonArray res = new JsonArray();
        array.forEach(value -> res.add(value));
        return res;
    }

    public void addTestReport(TestMethodReport report) {
        appendTestResult(gson.toJsonTree(report).getAsJsonObject(), readReport());
    }

}

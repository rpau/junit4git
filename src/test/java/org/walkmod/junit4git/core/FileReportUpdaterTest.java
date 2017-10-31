package org.walkmod.junit4git.core;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.walkmod.junit4git.core.reports.FileReportUpdater;
import org.walkmod.junit4git.junit4.Junit4GitRunner;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@RunWith(Junit4GitRunner.class)
public class FileReportUpdaterTest {

    @Test
    public void whenTheReportIsEmptyThenItWritesTheTest() {

        final StringWriter writer = new StringWriter();

        FileReportUpdater updaterOnEmptyReport = new FileReportUpdater() {

            @Override
            protected Writer buildWriter() throws IOException {
                return writer;
            }

            @Override
            protected Set<String> getReferencedClasses() {
                return new LinkedHashSet<>(Arrays.asList("Hello"));
            }

            @Override
            protected JsonArray readReport() {
                return new JsonArray();
            }
        };

        updaterOnEmptyReport.update("end", "MyClass", "mytest");

        String updated = writer.toString();

        Assert.assertEquals("[\n" +
                "  {\n" +
                "    \"test\": \"MyClass\",\n" +
                "    \"method\": \"mytest\",\n" +
                "    \"classes\": [\n" +
                "      \"Hello\"\n" +
                "    ]\n" +
                "  }\n" +
                "]", updated);
    }

    @Test
    public void whenTheReportIsNotEmptyThenItAppendsTheTest() {

        final StringWriter writer = new StringWriter();

        FileReportUpdater updaterWithNonEmptyReport = new FileReportUpdater() {

            @Override
            protected Writer buildWriter() throws IOException {
                return writer;
            }

            @Override
            protected Set<String> getReferencedClasses() {
                return new LinkedHashSet<>(Arrays.asList("Hello"));
            }

            @Override
            protected JsonArray readReport() {
                JsonObject object = new JsonObject();
                object.addProperty("test", "FooTest");
                object.addProperty("method", "test");
                object.add("classes", new JsonArray());
                JsonArray array = new JsonArray();
                array.add(object);
                return array;
            }
        };

        updaterWithNonEmptyReport.update("end", "MyClass", "mytest");

        String updated = writer.toString();

        Assert.assertEquals("[\n" +
                "  {\n" +
                "    \"test\": \"FooTest\",\n" +
                "    \"method\": \"test\",\n" +
                "    \"classes\": []\n" +
                "  },\n" +
                "  {\n" +
                "    \"test\": \"MyClass\",\n" +
                "    \"method\": \"mytest\",\n" +
                "    \"classes\": [\n" +
                "      \"Hello\"\n" +
                "    ]\n" +
                "  }\n" +
                "]", updated);
    }
}

package org.walkmod.junit4git.core.reports;


import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Class to construct a report as a list of TestMethodReports. By default, it will be stored as a json format.
 * The purpose of this class is to separate an specific storage (e.g using git notes or a file format)
 * from the API.
 */
public abstract class AbstractTestReportStorage {

  protected Gson gson = new GsonBuilder().setPrettyPrinting().create();

  /**
   * Method called before any read or writing operation; whose purpose
   * is to initialize/prepare the required resources.
   */
  public void prepare() {
    try (Writer writer = buildWriter()) {
      writer.write("");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Reads the test impact report
   *
   * @return the test impact report
   * @throws IOException
   */
  public abstract TestMethodReport[] getBaseReport() throws IOException;

  /**
   * Returns a reader to obtain the base report. Used to incrementally add news reports
   *
   * @return a reader to obtain the base report.
   * @throws IOException
   */
  protected abstract Reader buildReader() throws IOException;

  /**
   * Returns a writer to update the base report. Used to incrementally add news reports
   *
   * @return a writer to update the base report.
   * @throws IOException
   */
  protected abstract Writer buildWriter() throws IOException;

  /**
   * If exists an equivalent report previously calculated
   *
   * @return if exists an equivalent report previously calculated.
   * @throws IOException
   */
  protected abstract boolean isReportCreated() throws IOException;

  /**
   * Appends a test result into the report
   *
   * @param report test result to append
   */
  public void appendTestReport(TestMethodReport report) {
    appendTestReport(gson.toJsonTree(report).getAsJsonObject(), jsonReport());
  }

  private void appendTestReport(JsonObject object, JsonArray baseReport) {
    baseReport.add(object);
    try (Writer writer = buildWriter()) {
      writer.write(gson.toJson(baseReport));
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private JsonArray jsonReport() {
    JsonArray tests = new JsonArray();
    try {
      if (isReportCreated()) {
        JsonElement element = new JsonParser().parse(buildReader());
        if (element.isJsonArray()) {
          tests = element.getAsJsonArray();
        }
      }
    } catch (Exception e) {
    }
    return tests;
  }
}

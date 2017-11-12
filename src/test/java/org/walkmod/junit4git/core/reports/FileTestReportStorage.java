package org.walkmod.junit4git.core.reports;


import com.google.gson.Gson;

import java.io.*;

public class FileTestReportStorage extends AbstractTestReportStorage {


  private File report = new File("smart-testing-report.json");

  private File baseReport = new File("smart-testing-report.base.json");

  @Override
  public void prepare() {
    try (Writer writer = buildWriter()) {
      writer.write("");
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public TestMethodReport[] getBaseReport() throws IOException {
    return new Gson().fromJson(new FileReader(baseReport), TestMethodReport[].class);
  }

  @Override
  protected Reader buildReader() throws IOException {
    return new InputStreamReader(new FileInputStream(report));
  }

  @Override
  protected Writer buildWriter() throws IOException {
    return new FileWriter(report);
  }

  @Override
  protected boolean isReportCreated() throws IOException {
    return report.exists();
  }

}

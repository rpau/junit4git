package org.walkmod.junit4git.core.reports;



import java.io.*;

public class FileReportUpdater extends AbstractReportUpdater {


    private File report = new File("smart-testing-report.json");

    private File baseReport = new File("smart-testing-report.base.json");

    @Override
    public InputStream getBaseReport() throws IOException {
        return new FileInputStream(baseReport);
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

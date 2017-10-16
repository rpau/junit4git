package resolvers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class HttpTestsResolver {

    private String host;

    private String repoId;

    private String baseBranch;

    private String authKey;

    private OkHttpClient client;

    public HttpTestsResolver(String host, String repoId, String baseBranch, String authKey) {
        this(host, repoId, baseBranch, authKey, new OkHttpClient());
    }

    public HttpTestsResolver() throws ServerSetupException {
        this(getValueOf("SMART_TESTING_HOST"), getValueOf("SMART_TESTING_REPO_ID"),
                getValueOf("SMART_TESTING_REPO_BRANCH"), getValueOf("SMART_TESTING_HOST_AUTH"),
                new OkHttpClient());
    }

    private static String getValueOf(String key) throws ServerSetupException {
        String value = System.getenv(key);
        if (value == null) {
            throw new ServerSetupException();
        }
        return value;
    }

    public HttpTestsResolver(String host, String repoId, String baseBranch, String authKey,
                             OkHttpClient client) {
        this.host = host;
        this.repoId = repoId;
        this.baseBranch = baseBranch;
        this.authKey = authKey;
        this.client = client;
    }

    private String buildURL() throws IOException, GitAPIException {
        Git git = Git.open(new File("."));
        Status status = git.status().call();
        Set<String> changed = status.getChanged();

        JsonArray ja = new JsonArray();
        Iterator<String> it = changed.iterator();

        while(it.hasNext()) {
            ja.add(it.next());
        }
        return host +
                "?modified=" + URLEncoder.encode(ja.toString())
                + "&repoId=" + repoId
                + "&baseBranch=" + baseBranch;
    }

    private List<String> readFromJson(String data) {
        Iterator<JsonElement> itData = new JsonParser().parse(data).getAsJsonArray().iterator();
        List<String> result = new LinkedList<>();
        while(itData.hasNext()) {
            result.add(itData.next().getAsString());
        }
        return result;
    }

    public List<String> getTestsToIgnore() throws IOException, GitAPIException {
        Request request = new Request.Builder()
                .url(buildURL()).build();
        Response response = client.newCall(request).execute();
        return readFromJson(response.body().string());
    }

}

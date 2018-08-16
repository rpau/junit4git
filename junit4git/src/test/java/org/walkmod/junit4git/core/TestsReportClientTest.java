package org.walkmod.junit4git.core;


import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import okio.BufferedSink;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TestsReportClientTest {

  @Test
  public void it_produces_a_valid_request() throws IOException {
    OkHttpClient http = mock(OkHttpClient.class);
    when(http.newCall(any())).thenReturn(mock(Call.class));

    TestsReportClient client = new TestsReportClient(http, false, false);
    ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
    client.sendRequestToClassLoggerAgent("FooClass", "bar",
            JUnitEventType.START.getName());
    verify(http).newCall(argumentCaptor.capture());

    BufferedSink sink = new Buffer();
    argumentCaptor.getValue().body().writeTo(sink);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    sink.buffer().copyTo(stream);

    String content = new String(stream.toByteArray());

    Assert.assertEquals(new Gson().toJson(
            new JUnitEvent(JUnitEventType.START.getName(), "FooClass", "bar")), content);
  }
}

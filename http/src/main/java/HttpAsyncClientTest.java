import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.util.EntityUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpAsyncClientTest {

    public static void main(String[] args) throws Exception {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(50000)
                .setSocketTimeout(50000)
                .setConnectionRequestTimeout(1000)
                .build();

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                setIoThreadCount(Runtime.getRuntime().availableProcessors())
                .setSoKeepAlive(true)
                .build();
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(3);

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
//                .addInterceptorLast(new ResponseContentEncoding())
                .build();

        //start
        client.start();

        List<NameValuePair> tokenParams = new ArrayList<>(5);
        tokenParams.add(new BasicNameValuePair("env", "beta"));
        tokenParams.add(new BasicNameValuePair("keys",
                "iot-mqtt-broker.measurepoint.topic,iot-mqtt-broker.bull.name"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(tokenParams, Consts.UTF_8);
//        HttpGet get = new HttpGet("http://10.27.20.20:8090/config2/get?" + EntityUtils.toString(entity));

//        HttpGet get = new HttpGet("http://localhost:2080/");
//        client.execute(get, new CallBack()).get();

        HttpPost post = new HttpPost("http://localhost:2080/");
        post.setEntity(entity);
        client.execute(post, new CallBack()).get();


        System.out.println("print any key to exit ...");
        System.in.read();

        client.close();
    }

    static class CallBack implements FutureCallback<HttpResponse> {
        @Override
        public void completed(HttpResponse httpResponse) {
            try {
                HttpEntity rspEntity = httpResponse.getEntity();
//                Gson gson = new GsonBuilder().create();
//                ContentType contentType = ContentType.getOrDefault(rspEntity);
//                Charset charset = contentType.getCharset();
//                try (Reader reader = new InputStreamReader(rspEntity.getContent(), charset)) {
//                    System.out.println("received: \n" + gson.fromJson(reader, Map.class));
//                }
                System.out.println(rspEntity.getContentLength());
                byte[] data = new byte[1024];
                int length = new GZIPInputStream(rspEntity.getContent()).read(data);
                System.out.println(new String(data, 0, length));
            } catch (Exception e) {
                System.err.println("captured error: " + e.getMessage());
            }
        }

        @Override
        public void failed(Exception e) {
            System.err.println("request failed with error: " + e.getMessage());
        }

        @Override
        public void cancelled() {
            System.err.println("request cancelled");
        }
    }

}
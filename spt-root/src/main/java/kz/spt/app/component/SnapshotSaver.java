package kz.spt.app.component;


import kz.spt.app.service.CameraService;
import kz.spt.lib.model.Camera;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.val;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SnapshotSaver {


    @Value("${images.file.path}")
    String imagePath;

    private final CameraService cameraService;

    @Scheduled(fixedDelay = 2000)
    public void runner() {
        List<Camera> cameraList = cameraService.cameraList();
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Camera>> futures = new ArrayList<Future<Camera>>();
        for (final Camera camera : cameraList) {
            futures.add(executorService.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    getSnapshot(camera);
                    return null;
                }
            }));
        }
        for (Future<Camera> future : futures) {
            try {
                future.get();
            } catch (Exception ex) {
                // Do something
            }
        }

    }


    private void getSnapshot(Camera camera) throws Exception {

        String ip = camera.getIp();
        HttpHost host = new HttpHost(ip, 8080, "http");
        CloseableHttpClient client = HttpClientBuilder.create().
                setDefaultCredentialsProvider(provider(camera.getLogin(), camera.getPassword()))
                .useSystemProperties()
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpRequestFactoryDigestAuth(host, client);
        requestFactory.setConnectTimeout(2000);
        requestFactory.setConnectionRequestTimeout(2000);
        requestFactory.setReadTimeout(2000);
        var restTemplate = new RestTemplate(requestFactory);
        val headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        StringBuilder address = new StringBuilder();
        address.append("http://");
        address.append(ip);
        address.append(camera.getSnapshotUrl());
        HttpEntity entity = new HttpEntity(headers);
        byte[] imageBytes = restTemplate.getForObject(address.toString(), byte[].class, entity);
        Files.write(Paths.get(imagePath+"/"+ip.replace(".", "-") + ".jpeg"), imageBytes);
        requestFactory.destroy();
    }

    private CredentialsProvider provider(String login, String password) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(login, password);
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }

}

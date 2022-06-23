package kz.spt.app.component;


import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
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
import org.pf4j.util.StringUtils;
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

    @Scheduled(fixedDelay = 2000)
    public void runner() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<CameraStatusDto>> futures = new ArrayList<>();
        for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
            CameraStatusDto cameraStatusDto = gateStatusDto.frontCamera;
            if(cameraStatusDto != null &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto.login) &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto.password) &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto.snapshotUrl)){
                if(cameraStatusDto.snapshotEnabled!=null && !cameraStatusDto.snapshotEnabled) return;

                futures.add(executorService.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        getSnapshot(cameraStatusDto);
                        return null;
                    }
                }));
            }
            CameraStatusDto cameraStatusDto2 = gateStatusDto.frontCamera2;
            if(cameraStatusDto2 != null &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto2.login) &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto2.password) &&
                    StringUtils.isNotNullOrEmpty(cameraStatusDto2.snapshotUrl)){
                if(cameraStatusDto2.snapshotEnabled!=null && !cameraStatusDto2.snapshotEnabled) return;

                futures.add(executorService.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        getSnapshot(cameraStatusDto2);
                        return null;
                    }
                }));
            }
            CameraStatusDto backCameraStatusDto = gateStatusDto.backCamera;
            if(backCameraStatusDto != null &&
                    StringUtils.isNotNullOrEmpty(backCameraStatusDto.login) &&
                    StringUtils.isNotNullOrEmpty(backCameraStatusDto.password) &&
                    StringUtils.isNotNullOrEmpty(backCameraStatusDto.snapshotUrl)){
                if(backCameraStatusDto.snapshotEnabled!=null && !backCameraStatusDto.snapshotEnabled) return;

                futures.add(executorService.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        getSnapshot(backCameraStatusDto);
                        return null;
                    }
                }));
            }
        }

        for (Future<CameraStatusDto> future : futures) {
            try {
                future.get();
            } catch (Exception ex) {
                // Do something
            }
        }

    }

    private void getSnapshot(CameraStatusDto cameraStatusDto) throws Exception {
        String ip = cameraStatusDto.ip;
        HttpHost host = new HttpHost(ip, 8080, "http");
        CloseableHttpClient client = HttpClientBuilder.create().
                setDefaultCredentialsProvider(provider(cameraStatusDto.login, cameraStatusDto.password))
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
        address.append(cameraStatusDto.snapshotUrl);
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

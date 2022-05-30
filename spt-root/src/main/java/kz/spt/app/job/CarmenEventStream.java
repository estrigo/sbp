package kz.spt.app.job;

import kz.spt.app.job.StatusCheckJob;
import kz.spt.app.model.dto.CameraStatusDto;
import kz.spt.app.model.dto.GateStatusDto;
import kz.spt.lib.model.dto.carmen.CarmenImage;
import kz.spt.lib.service.CarEventService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.pf4j.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Log
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CarmenEventStream {
    @Value("${carmen.live.enabled}")
    Boolean carmenLiveEnabled;

    private final CarEventService carEventService;
    private Map<String, Future> streams = new HashMap<>();

    @SneakyThrows
    //@Scheduled(fixedDelay = 2000)
    public void run() {
        if(!carmenLiveEnabled) return;

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (GateStatusDto gateStatusDto : StatusCheckJob.globalGateDtos) {
            CameraStatusDto frontCamera = gateStatusDto.frontCamera;
            if (frontCamera != null &&
                    StringUtils.isNotNullOrEmpty(frontCamera.carmenLogin) &&
                    StringUtils.isNotNullOrEmpty(frontCamera.carmenPassword) &&
                    StringUtils.isNotNullOrEmpty(frontCamera.carmenIp)) {

                if (streams.containsKey(frontCamera.carmenIp)) continue;

                Future task = executorService.submit((Callable) () -> {
                    stream(frontCamera.carmenIp, frontCamera.carmenLogin, frontCamera.carmenPassword);
                    return null;
                });

                try {
                    task.get();
                    streams.put(frontCamera.carmenIp, task);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            CameraStatusDto frontCamera2 = gateStatusDto.frontCamera2;
            if (frontCamera2 != null &&
                    StringUtils.isNotNullOrEmpty(frontCamera2.carmenLogin) &&
                    StringUtils.isNotNullOrEmpty(frontCamera2.carmenPassword) &&
                    StringUtils.isNotNullOrEmpty(frontCamera2.carmenIp)) {

                if (streams.containsKey(frontCamera2.carmenIp)) continue;

                Future task = executorService.submit((Callable) () -> {
                    stream(frontCamera2.carmenIp, frontCamera2.carmenLogin, frontCamera2.carmenPassword);
                    return null;
                });

                try {
                    task.get();
                    streams.put(frontCamera2.carmenIp, task);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            CameraStatusDto backCamera = gateStatusDto.backCamera;
            if (backCamera != null &&
                    StringUtils.isNotNullOrEmpty(backCamera.carmenLogin) &&
                    StringUtils.isNotNullOrEmpty(backCamera.carmenPassword) &&
                    StringUtils.isNotNullOrEmpty(backCamera.carmenIp)) {

                if (streams.containsKey(backCamera.carmenIp)) continue;

                Future task = executorService.submit((Callable) () -> {
                    stream(backCamera.carmenIp, backCamera.carmenLogin, backCamera.carmenPassword);
                    return null;
                });

                try {
                    task.get();
                    streams.put(backCamera.carmenIp, task);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void stream(String ip, String user, String password) {
        String url = String.format("http://%s/live/events?user=%s&password=%s", ip, user, password);
        ConnectionKeepAliveStrategy keepAliveStrategy = (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 5 * 1000;
        };
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        try (CloseableHttpClient client = HttpClients.custom()
                .setKeepAliveStrategy(keepAliveStrategy)
                .setConnectionManager(connManager)
                .build()) {
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                String boundary = "--IPCamEventStreamBoundary";
                String headerEnd = "\r\n\r\n";

                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream inputStream = entity.getContent()) {
                        int b = inputStream.read();

                        String headerAccumulator = "";
                        List<Byte> binaryContentAccumulator = new ArrayList<>();
                        int expectedContentLength = -1;

                        Map<String, String> header = null;
                        CarmenImage eventPack = null;

                        Map<String, CarmenImage> imageBuffer = new HashMap<>();

                        while (b > -1) {
                            if (header == null) {
                                headerAccumulator += new String(new byte[]{(byte) b});
                                int indexOfBoundary = headerAccumulator.indexOf(boundary);
                                if (indexOfBoundary > -1) {
                                    int indexOfHeaderEnd = headerAccumulator.indexOf(headerEnd);
                                    if (indexOfHeaderEnd > -1) {
                                        int headerStartIndex = indexOfBoundary + boundary.length();
                                        header = parseHeader(headerAccumulator.substring(headerStartIndex, indexOfHeaderEnd));
                                        expectedContentLength = Integer.valueOf(header.get("Content-Length"));
                                        headerAccumulator = "";
                                    }
                                }
                            } else {
                                binaryContentAccumulator.add((byte) b);
                                if (!binaryContentAccumulator.isEmpty() && binaryContentAccumulator.size() >= expectedContentLength) {
                                    eventPack = CarmenImage.builder()
                                            .header(header)
                                            .content(new ArrayList<>(binaryContentAccumulator))
                                            .build();
                                    header = null;
                                    expectedContentLength = -1;
                                    binaryContentAccumulator.clear();
                                }
                            }

                            if (eventPack != null) {
                                String timestamp = eventPack.header.get("X-Timestamp");
                                String contentType = eventPack.header.get("Content-Type");
                                switch (contentType) {
                                    case "image/jpeg":
                                        imageBuffer.put(timestamp, eventPack);
                                        //log.info("[Image] " + date);
                                        break;
                                    case "application/json":
                                        byte[] arr = new byte[eventPack.content.size()];
                                        for (int i = 0; i < eventPack.content.size(); i++) {
                                            arr[i] = eventPack.content.get(i);
                                        }
                                        String eventDescriptor = new String(arr);
                                        CarmenImage image = imageBuffer.containsKey(timestamp) ? imageBuffer.get(timestamp) : null;
                                        for (String k : imageBuffer.keySet().stream().filter(m -> m.equals(timestamp)).collect(Collectors.toList())) {
                                            imageBuffer.remove(k);
                                        }

                                        byte[] img;
                                        if (image != null && image.content.size() > 0) {
                                            img = new byte[image.content.size()];
                                            for (int i = 0; i < image.content.size(); i++) {
                                                img[i] = image.content.get(i);
                                            }
                                        } else {
                                            img = new byte[0];
                                        }

                                        carEventService.handleLiveStreamEvent(img, eventDescriptor, timestamp);
                                        break;
                                }

                                eventPack = null;
                            }

                            b = inputStream.read();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        streams.remove(ip);
    }

    private Map<String, String> parseHeader(String headerContent) {
        if (headerContent.length() <= 0) return null;

        Map<String, String> header = new HashMap<>();
        var lines = headerContent.split("\n", 100);
        for (String h : lines) {
            var parts = h.split(":", 2);
            if (parts.length != 2) continue;
            header.put(parts[0], parts[1].replaceAll(" ", "").replaceAll("\r", ""));
        }
        return header;
    }
}

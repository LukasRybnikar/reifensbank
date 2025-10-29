package com.task.reifensbank.service.storage;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient client;
    private final Environment env;

    private String bucket;
    private boolean autoCreate;
    private final java.util.concurrent.atomic.AtomicBoolean bucketChecked = new java.util.concurrent.atomic.AtomicBoolean(false);

    @PostConstruct
    void init() {
        this.bucket = env.getProperty("app.storage.minio.bucket", "files");
        this.autoCreate = Boolean.parseBoolean(env.getProperty("app.storage.minio.auto-create-bucket", "true"));
        // ŽIADNY sieťový call tu – iba načítanie configu.
        log.info("MinIO configured for bucket='{}', endpoint='{}'", bucket, env.getProperty("app.storage.minio.endpoint"));
    }

    private void ensureBucketIfNeeded() throws Exception {
        if (bucketChecked.get()) return;
        synchronized (bucketChecked) {
            if (bucketChecked.get()) return;
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists && autoCreate) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket '{}'", bucket);
                }
                bucketChecked.set(true);
            } catch (Exception e) {
                String endpoint = env.getProperty("app.storage.minio.endpoint");
                String hint = """
                        MinIO endpoint nedostupný. Skontroluj:
                        - app.storage.minio.endpoint = %s (lokálne: http://localhost:9000, v Compose: http://minio:9000)
                        - či MinIO beží a port 9000 je dostupný
                        - sieť/DNS (hostname 'minio' funguje iba v Compose)
                        """.formatted(endpoint);
                log.error("MinIO init failed: {}", hint, e);
                throw e;
            }
        }
    }

    @Override
    public String put(String objectKey, MultipartFile file) throws Exception {
        ensureBucketIfNeeded();
        try (InputStream in = file.getInputStream()) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        }
    }

    @Override
    public void delete(String objectKey) throws Exception {
        ensureBucketIfNeeded();
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }
}

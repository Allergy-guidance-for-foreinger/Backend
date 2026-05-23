package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.MenuImageStoragePort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Component
public class FirebaseMenuImageStorageAdapter implements MenuImageStoragePort {

    private final MealCrawlProperties properties;
    private volatile Storage storage;

    public FirebaseMenuImageStorageAdapter(MealCrawlProperties properties) {
        this.properties = properties;
    }

    @Override
    public String upload(Long userId, MultipartFile imageFile) {
        try {
            String extension = resolveExtension(imageFile.getOriginalFilename(), imageFile.getContentType());
            String objectPath = buildObjectPath(userId, extension);
            String bucket = properties.getMenuImage().getFirebase().getBucketName();

            BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectPath)
                    .setContentType(imageFile.getContentType())
                    .build();
            getOrCreateStorage().create(blobInfo, imageFile.getInputStream());
            return objectPath;
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR, e);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if ("image/png".equals(contentType)) return "png";
        if ("image/webp".equals(contentType)) return "webp";
        return "jpg";
    }

    String buildObjectPath(Long userId, String extension) {
        return "menu-analysis/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private Storage createStorage(String credentialsPath) {
        try (InputStream inputStream = new FileInputStream(credentialsPath)) {
            return StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR, e);
        }
    }

    private Storage getOrCreateStorage() {
        Storage local = storage;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (storage == null) {
                String credentialsPath = properties.getMenuImage().getFirebase().getCredentialsPath();
                storage = createStorage(credentialsPath);
            }
            return storage;
        }
    }
}

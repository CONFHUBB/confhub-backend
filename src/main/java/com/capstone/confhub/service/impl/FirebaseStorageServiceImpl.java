package com.capstone.confhub.service.impl;

import com.capstone.confhub.service.FirebaseStorageService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    private final StorageClient storageClient;

    private static final String BUCKET_NAME = "filestorage-71871.firebasestorage.app";
    private static final String BASE_URL =
            "https://firebasestorage.googleapis.com/v0/b/" + BUCKET_NAME + "/o/";

    @Override
    public String uploadFile(MultipartFile file, Integer conferenceId, Integer paperId) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storagePath = "conferences/" + conferenceId + "/papers/" + paperId
                + "/" + UUID.randomUUID() + extension;

        Bucket bucket = storageClient.bucket();
        Blob blob = bucket.create(storagePath, file.getBytes(), file.getContentType());

        String downloadToken = getDownloadToken(blob);
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8);
        String downloadUrl = BASE_URL + encodedPath + "?alt=media&token=" + downloadToken;

        log.info("Uploaded file to Firebase Storage path '{}'. URL: {}", storagePath, downloadUrl);
        return downloadUrl;
    }

    @Override
    public String getFileUrl(String storagePath) {
        Bucket bucket = storageClient.bucket();
        Blob blob = bucket.get(storagePath);
        if (blob == null || !blob.exists()) {
            log.warn("File not found in Firebase Storage: {}", storagePath);
            return null;
        }

        String downloadToken = getDownloadToken(blob);
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8);
        return BASE_URL + encodedPath + "?alt=media&token=" + downloadToken;
    }

    private String getDownloadToken(Blob blob) {
        Map<String, String> metadata = blob.getMetadata();
        if (metadata != null && metadata.containsKey("firebaseStorageDownloadTokens")) {
            return metadata.get("firebaseStorageDownloadTokens");
        }
        // Generate a new download token and persist it on the blob
        String newToken = UUID.randomUUID().toString();
        Map<String, String> newMetadata = metadata != null ? new java.util.HashMap<>(metadata) : new java.util.HashMap<>();
        newMetadata.put("firebaseStorageDownloadTokens", newToken);
        blob.toBuilder().setMetadata(newMetadata).build().update();
        return newToken;
    }

    @Override
    public String uploadImage(MultipartFile file, Integer conferenceId) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Image file must not be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storagePath = "conferences/" + conferenceId + "/banners/"
                + UUID.randomUUID() + extension;

        Bucket bucket = storageClient.bucket();
        Blob blob = bucket.create(storagePath, file.getBytes(), file.getContentType());

        String downloadToken = getDownloadToken(blob);
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8);
        String downloadUrl = BASE_URL + encodedPath + "?alt=media&token=" + downloadToken;

        log.info("Uploaded banner image to Firebase Storage path '{}'. URL: {}", storagePath, downloadUrl);
        return downloadUrl;
    }

    @Override
    public String uploadChatFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("File must not be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storagePath = "chat-files/" + UUID.randomUUID() + extension;

        Bucket bucket = storageClient.bucket();
        Blob blob = bucket.create(storagePath, file.getBytes(), file.getContentType());

        String downloadToken = getDownloadToken(blob);
        String encodedPath = URLEncoder.encode(storagePath, StandardCharsets.UTF_8);
        String downloadUrl = BASE_URL + encodedPath + "?alt=media&token=" + downloadToken;

        log.info("Uploaded chat file to Firebase Storage path '{}'. URL: {}", storagePath, downloadUrl);
        return downloadUrl;
    }
}

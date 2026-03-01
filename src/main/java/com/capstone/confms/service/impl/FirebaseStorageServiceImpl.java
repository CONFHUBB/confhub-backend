package com.capstone.confms.service.impl;

import com.capstone.confms.service.FirebaseStorageService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

public class FirebaseStorageServiceImpl implements FirebaseStorageService {
    private StorageClient storageClient;

    private final String BASE_URL = "https://firebasestorage.googleapis.com/v0/b/swp391-1f20e.appspot.com/o/";

    public String uploadFile(MultipartFile file) throws IOException {

        Bucket bucket = storageClient.bucket();
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) return "Fail to upload";

        String[] split = file.getOriginalFilename().split("\\.");

        if (!split[split.length - 1].equals("jpg")
            && !split[split.length - 1].equals("jpeg")
            && !split[split.length - 1].equals("png")
        ) return "Fail to upload";

        String fileName = UUID.randomUUID().toString() + "." + split[split.length - 1];
        Blob blob = bucket.create(fileName, file.getBytes(), file.getContentType());

        return fileName;
    }

    public List<String> getImagesURL(String listImages) {
        List<String> response = new ArrayList<>();

        if (listImages == null || listImages.isEmpty()) return response;

        String[] split = listImages.split(",");

        for (String s : split) {

            String url = BASE_URL + s;
            RestTemplate restTemplate = new RestTemplate();

            try {
                JSONObject json = restTemplate.getForObject(url, JSONObject.class);
                String token = (String) Objects.requireNonNull(json).get("downloadTokens");

                if (token != null) response.add(url + "?alt=media&token=" +token);
            } catch (Exception ignored) {}
        }

        return response;
    }
}

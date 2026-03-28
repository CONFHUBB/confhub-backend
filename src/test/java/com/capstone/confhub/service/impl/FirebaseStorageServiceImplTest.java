package com.capstone.confhub.service.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseStorageServiceImplTest {

    @Mock
    private StorageClient storageClient;
    @Mock
    private Bucket bucket;
    @Mock
    private Blob blob;

    @InjectMocks
    private FirebaseStorageServiceImpl firebaseStorageService;

    @Test
    void shouldCreateService() {
        assertNotNull(firebaseStorageService);
    }

    @Test
    void uploadFileShouldReturnDownloadUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "paper.pdf", "application/pdf", "hello".getBytes());
        when(storageClient.bucket()).thenReturn(bucket);
        when(bucket.create(any(String.class), eq(file.getBytes()), eq("application/pdf"))).thenReturn(blob);
        when(blob.getMetadata()).thenReturn(Map.of("firebaseStorageDownloadTokens", "token123"));

        var result = firebaseStorageService.uploadFile(file, 1, 2);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.contains("token123"));
    }

    @Test
    void getFileUrlShouldReturnDownloadUrl() {
        when(storageClient.bucket()).thenReturn(bucket);
        when(bucket.get("conferences/1/papers/2/file.pdf")).thenReturn(blob);
        when(blob.exists()).thenReturn(true);
        when(blob.getMetadata()).thenReturn(Map.of("firebaseStorageDownloadTokens", "token123"));

        var result = firebaseStorageService.getFileUrl("conferences/1/papers/2/file.pdf");

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result.contains("token123"));
    }
}




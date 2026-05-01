package com.capstone.confhub.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FirebaseStorageService {
    /**
     * Uploads a file to Firebase Storage under conferences/{conferenceId}/papers/{paperId}/
     * and returns the public download URL.
     *
     * @param file         the multipart file to upload
     * @param conferenceId the ID of the conference this file belongs to
     * @param paperId      the ID of the paper this file belongs to
     * @return the public download URL of the uploaded file
     * @throws IOException if the upload fails
     */
    String uploadFile(MultipartFile file, Integer conferenceId, Integer paperId) throws IOException;

    /**
     * Returns the public download URL for an already-stored file name.
     *
     * @param fileName the stored file name (as returned by uploadFile)
     * @return the public download URL, or null if not found
     */
    String getFileUrl(String fileName);

    /**
     * Uploads an image to Firebase Storage under conferences/{conferenceId}/banners/
     * and returns the public download URL.
     */
    String uploadImage(MultipartFile file, Integer conferenceId) throws IOException;

    /**
     * Uploads a chat attachment to Firebase Storage under chat-files/
     * and returns the public download URL.
     */
    String uploadChatFile(MultipartFile file) throws IOException;

    /**
     * Uploads a paper template to Firebase Storage under conferences/{conferenceId}/paper-templates/
     * and returns the public download URL.
     */
    String uploadPaperTemplateFile(MultipartFile file, Integer conferenceId) throws IOException;
}

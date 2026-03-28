package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.UserConflictRequest;
import com.capstone.confhub.dto.response.UserConflictResponseDTO;

import java.util.List;

public interface UserConflictService {
    List<UserConflictResponseDTO> getConflictsByUserId(Integer userId);

    UserConflictResponseDTO addConflict(Integer userId, UserConflictRequest request);

    void deleteConflict(Integer conflictId);

    UserConflictResponseDTO toggleConflictActive(Integer conflictId);
}

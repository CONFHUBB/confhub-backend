package com.capstone.confms.service;

import com.capstone.confms.dto.request.UserConflictRequest;
import com.capstone.confms.dto.response.UserConflictResponseDTO;

import java.util.List;

public interface UserConflictService {
    List<UserConflictResponseDTO> getConflictsByUserId(Integer userId);

    UserConflictResponseDTO addConflict(Integer userId, UserConflictRequest request);

    void deleteConflict(Integer conflictId);

    UserConflictResponseDTO toggleConflictActive(Integer conflictId);
}

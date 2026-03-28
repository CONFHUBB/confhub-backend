package com.capstone.confhub.dto;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.utils.enums.ConflictType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperConflictDTO {
    private Integer paperId;
    private Integer userId;
    private ConflictType conflictType;
}
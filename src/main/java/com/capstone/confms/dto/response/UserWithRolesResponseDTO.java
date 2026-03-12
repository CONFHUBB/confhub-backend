package com.capstone.confms.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserWithRolesResponseDTO {
    private UserResponseDTO user;
    private List<ConferenceUserTrackResponseDTO> roles;
}

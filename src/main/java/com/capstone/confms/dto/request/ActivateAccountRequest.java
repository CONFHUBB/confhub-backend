package com.capstone.confms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivateAccountRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String invitationToken;

    @NotBlank
    @Size(min = 6, max = 40)
    private String newPassword;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}

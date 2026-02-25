package com.capstone.confms.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class EmailDTO {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String to;


    @NotBlank(message = "Message subject cannot be blank")
    private String subject;


    @NotBlank(message = "Content cannot be blank")
    private String text;
}

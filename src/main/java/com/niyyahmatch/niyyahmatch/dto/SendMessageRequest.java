package com.niyyahmatch.niyyahmatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String content;
}

package org.solace.scholar_ai.user_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.user_service.model.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupDTO {
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = MIN_PASSWORD_LENGTH)
    private String password;

    private UserRole role = UserRole.USER;
}

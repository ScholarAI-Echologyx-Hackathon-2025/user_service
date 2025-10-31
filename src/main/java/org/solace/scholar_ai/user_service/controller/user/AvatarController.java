package org.solace.scholar_ai.user_service.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.user_service.dto.response.APIResponse;
import org.solace.scholar_ai.user_service.dto.user.AvatarUploadResponse;
import org.solace.scholar_ai.user_service.model.User;
import org.solace.scholar_ai.user_service.repository.UserRepository;
import org.solace.scholar_ai.user_service.service.user.AvatarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me/avatar")
@Tag(name = "Avatar Management", description = "Avatar upload and management endpoints (Cloudinary)")
@RequiredArgsConstructor
@Slf4j
public class AvatarController {

    private static final String SECURE_URL_KEY = "secure_url";
    private static final String PUBLIC_ID_KEY = "public_id";
    private static final String USER_NOT_FOUND_MSG = "User not found with email: ";
    private static final String AVATAR_UPLOAD_SUCCESS = "Avatar uploaded successfully";
    private static final String AVATAR_DELETE_SUCCESS = "Avatar deleted successfully";

    private final AvatarService avatarService;
    private final UserRepository userRepository;

    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Upload Avatar", description = "Upload avatar via multipart to Cloudinary and update profile")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Avatar uploaded",
                        content = @Content(schema = @Schema(implementation = AvatarUploadResponse.class))),
                @ApiResponse(responseCode = "400", description = "Validation error"),
                @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<AvatarUploadResponse>> upload(
            Principal principal, @RequestPart("file") MultipartFile file) {
        try {
            User user = getUserByPrincipal(principal);
            AvatarUploadResponse response = uploadAvatar(user, file);

            return ResponseEntity.ok(APIResponse.success(200, AVATAR_UPLOAD_SUCCESS, response));
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
    
    private User getUserByPrincipal(Principal principal) {
        String email = principal.getName();
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG + email));
    }
    
    private AvatarUploadResponse uploadAvatar(User user, MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        Map<String, Object> uploadResult = avatarService.uploadToCloudinary(
                user.getId(), file.getOriginalFilename(), file.getContentType(), bytes);
        
        String secureUrl = (String) uploadResult.get(SECURE_URL_KEY);
        String publicId = (String) uploadResult.get(PUBLIC_ID_KEY);

        avatarService.setAvatarFromCloudinary(user.getId(), publicId, secureUrl);
        
        return new AvatarUploadResponse(secureUrl, publicId);
    }

    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Delete Avatar", description = "Delete the current user's avatar")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Avatar deleted successfully"),
                @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
                @ApiResponse(responseCode = "404", description = "User not found")
            })
    @DeleteMapping
    public ResponseEntity<APIResponse<String>> deleteAvatar(Principal principal) {
        try {
            User user = getUserByPrincipal(principal);
            log.info("Delete avatar request for user: {}", user.getEmail());

            avatarService.deleteCurrentAvatar(user.getId());

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(APIResponse.success(HttpStatus.NO_CONTENT.value(), AVATAR_DELETE_SUCCESS, null));
        } catch (Exception e) {
            log.error("Error deleting avatar: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
}

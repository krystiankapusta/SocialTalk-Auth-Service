package SocialTalk.Auth_Service.Controller;

import SocialTalk.Auth_Service.DataTransferObject.LoginUserDTO;
import SocialTalk.Auth_Service.DataTransferObject.RegisterUserDTO;
import SocialTalk.Auth_Service.Model.User;
import SocialTalk.Auth_Service.Responses.LoginResponse;
import SocialTalk.Auth_Service.Service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import SocialTalk.Auth_Service.Service.JwtService;
import SocialTalk.Auth_Service.DataTransferObject.VerifyUserDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RequestMapping("/auth")
@RestController
@Tag(name = "Authentication", description = "Auth Service API")
public class AuthenticationController {
    private final JwtService jwtService;
    @Autowired

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    @Operation(
            summary = "Register new user",
            description = "Creates a new user account with the provided registration details. Returns user data on success."
    )
    public ResponseEntity<?> register(@RequestBody RegisterUserDTO registerUserDTO) {
        try{
            User registeredUser = authenticationService.signup(registerUserDTO);
            return ResponseEntity.ok(registeredUser);
        } catch (AuthenticationService.EmailAlreadyRegisteredException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }

    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates user credentials and returns JWT token with expiration time"
    )
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDTO loginUserDTO){
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        Long userId = authenticatedUser.getId();
        String jwtToken = jwtService.generateToken(authenticatedUser, userId);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify user account",
            description = "Verifies user account using the verification code"
    )
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            authenticationService.verifyUser(verifyUserDTO);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    @Operation(
            summary = "Resend verification code",
            description = "Resends verification code to the specified email address"
    )
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resetPasswordRequest")
    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset link to the provided email address"
    )
    public ResponseEntity<?> resetPasswordRequest(@RequestParam String email, HttpServletResponse response)
    {
        try{
            authenticationService.sendResetPassword(email, response);
            return ResponseEntity.ok("Reset password link sent to your email");
        } catch (RuntimeException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/resetPassword")
    @Operation(
            summary = "Reset password",
            description = "Resets user password using the reset token and the new password provided"
    )
    public ResponseEntity<?> resetPassword(HttpServletRequest request, @RequestBody Map<String, String> requestBody) {

        String resetToken = extractTokenFromCookies(request);
        if (resetToken == null || resetToken.isBlank()) {
            return ResponseEntity.badRequest().body("Reset token is missing or invalid");
        }

        String newPassword = requestBody.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("New password is required");
        }

        try {
            authenticationService.resetPassword(resetToken, newPassword);
            return ResponseEntity.ok("Password has been reset successfully");
        } catch (RuntimeException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("resetToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        System.out.println("No resetToken cookie found");
        return null;
    }

}

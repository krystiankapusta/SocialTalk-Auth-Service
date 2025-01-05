package SocialTalk.Auth_Service.Controller;

import SocialTalk.Auth_Service.DataTransferObject.LoginUserDTO;
import SocialTalk.Auth_Service.DataTransferObject.RegisterUserDTO;
import SocialTalk.Auth_Service.Model.User;
import SocialTalk.Auth_Service.Responses.LoginResponse;
import SocialTalk.Auth_Service.Service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import SocialTalk.Auth_Service.Service.JwtService;
import SocialTalk.Auth_Service.DataTransferObject.VerifyUserDTO;

import java.util.Map;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    @Autowired

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
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
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDTO loginUserDTO){
        User authenticatedUser = authenticationService.authenticate(loginUserDTO);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDTO verifyUserDTO) {
        try {
            authenticationService.verifyUser(verifyUserDTO);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resetPasswordRequest")
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
    public ResponseEntity<?> resetPassword(
            @CookieValue(value = "resetToken", required = false) String resetToken,
            @RequestBody Map<String, String> requestBody) {

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

}

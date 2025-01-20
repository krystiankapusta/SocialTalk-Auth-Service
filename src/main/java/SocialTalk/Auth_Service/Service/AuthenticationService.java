package SocialTalk.Auth_Service.Service;

import SocialTalk.Auth_Service.DataTransferObject.LoginUserDTO;
import SocialTalk.Auth_Service.DataTransferObject.VerifyUserDTO;
import SocialTalk.Auth_Service.Model.User;
import SocialTalk.Auth_Service.DataTransferObject.RegisterUserDTO;
import SocialTalk.Auth_Service.Repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    @Value("${resetUrl}")
    private String resetUrl;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            JwtService jwtService)
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    public User signup(RegisterUserDTO input)
    {
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        if (userRepository.existsByEmail(user.getEmail()))
        {
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDTO input)
    {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isEnabled())
        {
            throw new RuntimeException("Account not verified. Please verify your account");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );
        return user;
    }

    public void verifyUser(VerifyUserDTO input)
    {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if(optionalUser.isPresent())
        {
            User user = optionalUser.get();
            if(user.getVerificationCodeExpiredAt().isBefore(LocalDateTime.now()))
            {
                throw new RuntimeException("Verification code has expired");
            }

            if(user.getVerificationCode().equals(input.getVerificationCode()))
            {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCode(null);
                userRepository.save(user);
            } else
            {
                throw new RuntimeException("Invalid verification code");
            }
        } else
        {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationCode(String email)
    {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent())
        {
            User user = optionalUser.get();
            if (user.isEnabled())
            {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiredAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void sendVerificationEmail(User user)
    {
        String subject = "Account verification";
        String verificationCode = user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    public void sendResetPassword(String email, HttpServletResponse response) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {

            String resetToken = jwtService.generateResetToken(email);

            Cookie resetTokenCookie = new Cookie("resetToken", resetToken);
            resetTokenCookie.setHttpOnly(false);
            resetTokenCookie.setSecure(false);
            resetTokenCookie.setPath("/");
            resetTokenCookie.setDomain("localhost");
            resetTokenCookie.setMaxAge(15 * 60);
            response.addCookie(resetTokenCookie);


            String subject = "Password Reset Request";
            String htmlMessage = "<html>"
                    + "<body>"
                    + "<p>Please click the link below to reset your password:</p>"
                    + "<a href=\"" + resetUrl + "\">Reset Password</a>"
                    + "</body>"
                    + "</html>";

            try {
                emailService.sendVerificationEmail(email, subject, htmlMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to send email");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resetPassword(String token, String newPassword) {

        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (RuntimeException e) {
            logger.error("Error extracting username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired reset token");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            logger.info("User not found for email: {}", email);
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();
        if (!jwtService.isResetTokenValid(token, user.getEmail())) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException(String message) {
            super(message);
        }
    }

}

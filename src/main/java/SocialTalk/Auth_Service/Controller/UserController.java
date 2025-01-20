package SocialTalk.Auth_Service.Controller;

import SocialTalk.Auth_Service.Model.User;
import SocialTalk.Auth_Service.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;


@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/me")
    @Operation(
            summary = "Get authenticated user",
            description = "Retrieves the currently authenticated user's information"
    )
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof User currentUser) {
            logger.info("Current user: {}", currentUser);
            return ResponseEntity.ok(currentUser);
        } else {
            logger.info("Principal is not of type User: {}", principal);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/find/{id}")
    @Operation(
            summary = "Find user by ID",
            description = "Retrieves a specific user's information based on their ID"
    )
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            logger.info("Controller: Received request for user ID: {}", id);
            Optional<User> user = userService.getUser(id);

            if (user.isPresent()) {
                logger.info("Controller: Found user: {}", user.get());
                return ResponseEntity.ok(user.get());
            } else {
                logger.info("Controller: No user found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Controller: Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @Operation(
            summary = "Get all users",
            description = "Retrieves a list of all registered users in the system"
    )
    public ResponseEntity<List<User>> allUsers() {
        List <User> users = userService.allUsers();

        for (User item : users) {
            logger.info("User: {}", item);
        }

        logger.info("All users: {}", users);

        return ResponseEntity.ok(users);
    }

}

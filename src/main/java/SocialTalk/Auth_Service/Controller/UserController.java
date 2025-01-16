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
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof User currentUser) {
            logger.debug("Current user: {}", currentUser);
            return ResponseEntity.ok(currentUser);
        } else {
            logger.warn("Principal is not of type User: {}", principal);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

//    @GetMapping("/findUser/{id}")
//    public Optional<User> getUser(@PathVariable Long id) {
//        logger.info("ID ", id);
//        logger.info("User found: ", userService.getUser(id));
//        return userService.getUser(id);
//    }

    @GetMapping("/find/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            System.out.println("Controller: Received request for user ID: " + id);
            Optional<User> user = userService.getUser(id);

            if (user.isPresent()) {
                System.out.println("Controller: Found user: " + user.get());
                return ResponseEntity.ok(user.get());
            } else {
                System.out.println("Controller: No user found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("Controller: Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> allUsers() {
        List <User> users = userService.allUsers();

        for (User item : users) {
            logger.info("User: {}", item);
        }

        logger.info("All users: {}", users);

        System.out.println("Users all" + users);
        return ResponseEntity.ok(users);
    }

}

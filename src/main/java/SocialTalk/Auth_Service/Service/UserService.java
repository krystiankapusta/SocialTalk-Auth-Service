package SocialTalk.Auth_Service.Service;

import org.slf4j.Logger;
import SocialTalk.Auth_Service.Model.User;
import SocialTalk.Auth_Service.Repository.UserRepository;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        System.out.println("UserService has been created");
    }


    public List<User> allUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public Optional<User> getUser(Long id) {
        try {
            logger.debug("Service: Looking up user with ID: {}", id);
            Optional<User> user = userRepository.findById(id);
            logger.debug("Service: User found: {}, Details: {}", user.isPresent(), user.orElse(null));
            return user;
        } catch (Exception e) {
            logger.error("Service: Error fetching user with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}

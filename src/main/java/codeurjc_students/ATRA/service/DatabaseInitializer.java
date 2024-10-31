package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

@Service
public class DatabaseInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostConstruct
    public void init() {
        User user = new User("pepe", "pass");

        userService.save(user);
    }
}

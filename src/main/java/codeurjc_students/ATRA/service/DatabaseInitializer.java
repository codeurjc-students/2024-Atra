package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DatabaseInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        User user = new User("asd", passwordEncoder.encode("asd"));
        user.setDisplayname("pepito");

        userService.save(user);
    }
}

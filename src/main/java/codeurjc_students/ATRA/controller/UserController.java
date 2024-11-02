package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	private UserService userService;

    public User getUser(){
        return null;
    }

    @PostMapping
    public ResponseEntity<User> createUser(){
       return null;
    }

    public ResponseEntity<User> modifyUser(){
        return null;
    }

    public ResponseEntity<User> deleteUser(){
        return null;
    }

}


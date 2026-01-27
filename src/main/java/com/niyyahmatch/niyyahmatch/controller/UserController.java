package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController()
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public User registerNewUser(@RequestBody User user){
        return userService.registerUser(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id){
        Optional<User> userOptional = userService.findUserById(id);
        if(userOptional.isPresent()){
            return userOptional.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
    }

}

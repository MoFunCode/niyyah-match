package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.dto.RegisterUserRequest;
import com.niyyahmatch.niyyahmatch.dto.UserResponse;
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
        public UserResponse registerNewUser(@RequestBody RegisterUserRequest request){
        // Step 1: Convert DTO → Entity
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .location(request.getLocation())
                .bio(request.getBio())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .build();

        User savedUser = userService.registerUser(user);
        // Convert Entity → DTO and return
        return new UserResponse(savedUser);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id){
        Optional<User> userOptional = userService.findUserById(id);
        if(userOptional.isPresent()){
            return new UserResponse(userOptional.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
    }

}

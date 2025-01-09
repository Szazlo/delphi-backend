package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.User;
import com.davidwilson.delphi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Boolean> isUsernameTaken(@PathVariable String username) {
        System.out.println(userService.getUserByUsername(username));
        return ResponseEntity.ok(userService.getUserByUsername(username).isPresent());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Boolean> isEmailTaken(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email).isPresent());
    }
}
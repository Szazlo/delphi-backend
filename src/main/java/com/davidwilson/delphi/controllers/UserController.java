package com.davidwilson.delphi.controllers;

import com.davidwilson.delphi.entities.User;
import com.davidwilson.delphi.services.UserService;
import com.davidwilson.delphi.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // DTO for handling profile picture updates
    public static class ProfilePictureRequest {
        private String pfp;

        public String getPfp() {
            return pfp;
        }

        public void setPfp(String pfp) {
            this.pfp = pfp;
        }
    }

    @GetMapping("/pfp/{userId}")
    public ResponseEntity<String> getProfilePicture(@PathVariable String userId) {
        return userRepository.findByUserId(userId)
                .map(User::getPfp)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pfp/{userId}")
    public ResponseEntity<String> setProfilePicture(@PathVariable String userId, @RequestBody ProfilePictureRequest request) {
        Optional<User> existingUser = userRepository.findByUserId(userId);

        User user = existingUser.orElse(new User());
        user.setUserId(userId);
        user.setPfp(request.getPfp());

        userRepository.save(user);
        return ResponseEntity.ok(request.getPfp());
    }
}
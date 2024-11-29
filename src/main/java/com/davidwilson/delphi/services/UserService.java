package com.davidwilson.delphi.services;

import aj.org.objectweb.asm.commons.Remapper;
import com.davidwilson.delphi.entities.User;
import com.davidwilson.delphi.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findAll().stream().filter(user -> user.getUsername().equals(username)).findFirst();
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}

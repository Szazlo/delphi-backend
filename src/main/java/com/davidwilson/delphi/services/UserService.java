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
}
package com.obs.service.Impl;


import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.exception.ConflictException;
import com.obs.exception.ResourceNotFoundException;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.request.UpdateUserRequest;
import com.obs.repository.UserRepository;

import com.obs.service.Interfaces.IUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User createBanker(SignupRequest signUpRequest) {
        if (existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }
        if (existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setFullName(signUpRequest.getFullName());
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        roles.add(Role.BANKER);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllNonAdminUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !u.getRoles().contains(Role.ADMIN))
                .toList();
    }

    @Override
    @Transactional
    public void toggleUserActive(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }


    @Override
    @Transactional
    public User saveNewUser(User user) {

        String pwd = user.getPassword();
        if (pwd == null || !pwd.startsWith("$2")) {
            user.setPassword(encoder.encode(pwd));
        }
        return userRepository.save(user);
    }


    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));
    }


    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateUserRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Error: User not found."));

        if (!user.getEmail().equals(updateRequest.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                throw new ConflictException("Error: Email is already in use!");
            }
        }

        user.setFullName(updateRequest.getFullName());
        user.setEmail(updateRequest.getEmail());
        user.setPhoneNumber(updateRequest.getPhoneNumber());

        userRepository.save(user);
    }


}

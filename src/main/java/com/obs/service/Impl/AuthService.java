package com.obs.service.Impl;

import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.exception.ConflictException;
import com.obs.payload.request.LoginRequest;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.response.JwtResponse;
import com.obs.security.JwtUtils;
import com.obs.security.services.UserDetailsImpl;

import com.obs.service.Interfaces.IAuthService;
import com.obs.service.Interfaces.IUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final IUserService userService;

    public AuthService(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponse signin(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getFullName(),
                userDetails.getEmail(),
                userDetails.getPhoneNumber(),
                roles
        );
    }

    @Override
    @Transactional
    public void signup(SignupRequest signUpRequest) {
        // Use UserService for existence checks (controller should not touch repo directly)
        if (userService.existsByUsername(signUpRequest.getUsername())) {
            throw new ConflictException("Username is already taken!");
        }
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            throw new ConflictException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(signUpRequest.getPassword()); // raw; UserService will encode
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setFullName(signUpRequest.getFullName());
        user.setActive(true);

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();
        if (strRoles == null || strRoles.isEmpty()) {
            roles.add(Role.CUSTOMER);
        } else {
            for (String r : strRoles) {
                switch (r.toLowerCase()) {
                    case "admin" -> roles.add(Role.ADMIN);
                    case "banker" -> roles.add(Role.BANKER);
                    default -> roles.add(Role.CUSTOMER);
                }
            }
        }
        user.setRoles(roles);

        userService.saveNewUser(user); // handles encoding & persistence
    }
}

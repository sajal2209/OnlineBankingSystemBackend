
package com.obs.controller;

import com.obs.payload.request.UpdateUserRequest;
import com.obs.payload.response.MessageResponse;
import com.obs.security.services.UserDetailsImpl;

import com.obs.service.Interfaces.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;

    // Constructor injection
    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateUserRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        userService.updateProfile(userId, updateRequest);

        return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
    }
}

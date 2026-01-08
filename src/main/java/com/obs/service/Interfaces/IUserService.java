package com.obs.service.Interfaces;


import com.obs.entity.User;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.request.UpdateUserRequest;

import java.util.List;

public interface IUserService {

    /**
     * Returns a user by username or throws if not found.
     */
    User getByUsername(String username);


    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    User createBanker(SignupRequest signUpRequest);
    List<User> getAllNonAdminUsers();
    void toggleUserActive(Long id);

    User saveNewUser(User user);


    User getById(Long id);

    void updateProfile(Long userId, UpdateUserRequest updateRequest);


}


package com.obs.service.Interfaces;


import com.obs.payload.request.LoginRequest;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.response.JwtResponse;

public interface IAuthService {

    JwtResponse signin(LoginRequest loginRequest);

    void signup(SignupRequest signUpRequest);
}


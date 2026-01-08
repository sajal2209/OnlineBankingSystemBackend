package com.obs.service.Impl;

import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.exception.ConflictException;
import com.obs.payload.request.LoginRequest;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.response.JwtResponse;
import com.obs.security.JwtUtils;
import com.obs.security.services.UserDetailsImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Mock
    JwtUtils jwtUtils;

    @Mock
    UserService userService; // concrete type used in AuthService constructor

    @InjectMocks
    AuthService authService;

    @Mock
    Authentication authentication;

    @Mock
    UserDetailsImpl userDetails;

    @Captor
    ArgumentCaptor<User> userCaptor;

    @Test
    @DisplayName("signin returns JwtResponse populated from authentication principal and token")
    void signin_success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("secret");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("the-jwt-token");

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(11L);
        when(userDetails.getUsername()).thenReturn("alice");
        when(userDetails.getFullName()).thenReturn("Alice A");
        when(userDetails.getEmail()).thenReturn("a@ex.com");
        when(userDetails.getPhoneNumber()).thenReturn("9999999999");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

        JwtResponse resp = authService.signin(req);

        assertEquals("the-jwt-token", resp.getToken());
        assertEquals(11L, resp.getId());
        assertEquals("alice", resp.getUsername());
        assertEquals("Alice A", resp.getFullName());
        assertEquals("a@ex.com", resp.getEmail());
        assertEquals("9999999999", resp.getPhoneNumber());
        assertTrue(resp.getRoles().contains("ROLE_USER"));

        verify(authenticationManager).authenticate(any());
        verify(jwtUtils).generateJwtToken(authentication);
    }

    @Test
    @DisplayName("signup throws ConflictException when username already exists")
    void signup_usernameConflict() {
        SignupRequest req = new SignupRequest();
        req.setUsername("bob");
        req.setEmail("b@ex.com");
        req.setPassword("pw");
        req.setPhoneNumber("1111111111");
        req.setFullName("Bob");

        when(userService.existsByUsername("bob")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.signup(req));
        assertTrue(ex.getMessage().contains("Username is already taken"));

        verify(userService).existsByUsername("bob");
        verify(userService, never()).existsByEmail(any());
        verify(userService, never()).saveNewUser(any());
    }

    @Test
    @DisplayName("signup throws ConflictException when email already exists")
    void signup_emailConflict() {
        SignupRequest req = new SignupRequest();
        req.setUsername("bob2");
        req.setEmail("b2@ex.com");
        req.setPassword("pw");
        req.setPhoneNumber("2222222222");
        req.setFullName("Bobby");

        when(userService.existsByUsername("bob2")).thenReturn(false);
        when(userService.existsByEmail("b2@ex.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.signup(req));
        assertTrue(ex.getMessage().contains("Email is already in use"));

        verify(userService).existsByUsername("bob2");
        verify(userService).existsByEmail("b2@ex.com");
        verify(userService, never()).saveNewUser(any());
    }

    @Test
    @DisplayName("signup with null roles defaults to CUSTOMER and saves user with fields set")
    void signup_nullRoles_defaultsCustomer() {
        SignupRequest req = new SignupRequest();
        req.setUsername("charlie");
        req.setEmail("c@ex.com");
        req.setPassword("pw");
        req.setPhoneNumber("3333333333");
        req.setFullName("Charlie");
        req.setRole(null);

        when(userService.existsByUsername("charlie")).thenReturn(false);
        when(userService.existsByEmail("c@ex.com")).thenReturn(false);
        when(userService.saveNewUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.signup(req);

        verify(userService).saveNewUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("charlie", saved.getUsername());
        assertEquals("c@ex.com", saved.getEmail());
        assertEquals("pw", saved.getPassword());
        assertEquals("3333333333", saved.getPhoneNumber());
        assertEquals("Charlie", saved.getFullName());
        assertTrue(saved.isActive());
        assertNotNull(saved.getRoles());
        assertEquals(1, saved.getRoles().size());
        assertTrue(saved.getRoles().contains(Role.CUSTOMER));

        verify(userService).saveNewUser(any());
    }

    @Test
    @DisplayName("signup with varied role strings maps to ADMIN, BANKER, CUSTOMER (case-insensitive)")
    void signup_variedRoles_mapping() {
        SignupRequest req = new SignupRequest();
        req.setUsername("dave");
        req.setEmail("d@ex.com");
        req.setPassword("pw");
        req.setPhoneNumber("4444444444");
        req.setFullName("Dave");
        Set<String> roles = new HashSet<>();
        roles.add("Admin");
        roles.add("banker");
        roles.add("somethingElse");
        req.setRole(roles);

        when(userService.existsByUsername("dave")).thenReturn(false);
        when(userService.existsByEmail("d@ex.com")).thenReturn(false);
        when(userService.saveNewUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        authService.signup(req);

        verify(userService).saveNewUser(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertNotNull(saved.getRoles());
        assertEquals(3, saved.getRoles().size());
        assertTrue(saved.getRoles().contains(Role.ADMIN));
        assertTrue(saved.getRoles().contains(Role.BANKER));
        assertTrue(saved.getRoles().contains(Role.CUSTOMER));

        verify(userService).saveNewUser(any());
    }
}

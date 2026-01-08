package com.obs.service.Impl;

import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.exception.ConflictException;
import com.obs.exception.ResourceNotFoundException;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.request.UpdateUserRequest;
import com.obs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder encoder;

    @InjectMocks
    UserService userService;

    @Captor
    ArgumentCaptor<User> userCaptor;

    @BeforeEach
    void setup() {
        // nothing to init beyond Mockito annotations
    }

    @Test
    @DisplayName("getByUsername should return user when found")
    void getByUsername_found() {
        User u = new User();
        u.setUsername("joe");
        when(userRepository.findByUsername("joe")).thenReturn(Optional.of(u));

        User actual = userService.getByUsername("joe");
        assertSame(u, actual);
    }

    @Test
    @DisplayName("getByUsername should throw ResourceNotFoundException when not found")
    void getByUsername_notFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getByUsername("missing"));
    }

    @Test
    @DisplayName("existsByUsername delegates to repository")
    void existsByUsername_delegates() {
        when(userRepository.existsByUsername("a")).thenReturn(Boolean.TRUE);
        assertTrue(userService.existsByUsername("a"));
        when(userRepository.existsByUsername("b")).thenReturn(Boolean.FALSE);
        assertFalse(userService.existsByUsername("b"));
    }

    @Test
    @DisplayName("existsByEmail delegates to repository")
    void existsByEmail_delegates() {
        when(userRepository.existsByEmail("x@x.com")).thenReturn(Boolean.TRUE);
        assertTrue(userService.existsByEmail("x@x.com"));
    }

    @Test
    @DisplayName("createBanker throws when username already exists")
    void createBanker_usernameConflict() {
        SignupRequest req = new SignupRequest();
        req.setUsername("u1");
        req.setEmail("u1@e.com");
        req.setPassword("pw");
        req.setPhoneNumber("123");
        req.setFullName("U One");

        when(userRepository.existsByUsername("u1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createBanker(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBanker throws when email already exists")
    void createBanker_emailConflict() {
        SignupRequest req = new SignupRequest();
        req.setUsername("u2");
        req.setEmail("u2@e.com");
        req.setPassword("pw");
        req.setPhoneNumber("123");
        req.setFullName("U Two");

        when(userRepository.existsByUsername("u2")).thenReturn(false);
        when(userRepository.existsByEmail("u2@e.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createBanker(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBanker creates user with encoded password and BANKER role")
    void createBanker_success() {
        SignupRequest req = new SignupRequest();
        req.setUsername("banker1");
        req.setEmail("b@e.com");
        req.setPassword("plainpw");
        req.setPhoneNumber("9999999999");
        req.setFullName("Banker One");

        when(userRepository.existsByUsername("banker1")).thenReturn(false);
        when(userRepository.existsByEmail("b@e.com")).thenReturn(false);
        when(encoder.encode("plainpw")).thenReturn("$2encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = userService.createBanker(req);

        verify(encoder).encode("plainpw");
        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();

        assertEquals("banker1", captured.getUsername());
        assertEquals("b@e.com", captured.getEmail());
        assertEquals("$2encoded", captured.getPassword());
        assertTrue(captured.getRoles().contains(Role.BANKER));
        assertSame(saved, captured);
    }

    @Test
    @DisplayName("getAllNonAdminUsers filters out admins")
    void getAllNonAdminUsers_filters() {
        User u1 = new User();
        u1.setUsername("u1");
        u1.setRoles(Set.of(Role.CUSTOMER));

        User u2 = new User();
        u2.setUsername("u2");
        u2.setRoles(Set.of(Role.ADMIN));

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> list = userService.getAllNonAdminUsers();
        assertEquals(1, list.size());
        assertEquals("u1", list.get(0).getUsername());
    }

    @Test
    @DisplayName("toggleUserActive flips active and saves")
    void toggleUserActive_success() {
        User u = new User();
        u.setId(5L);
        u.setActive(true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.toggleUserActive(5L);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertFalse(saved.isActive());
    }

    @Test
    @DisplayName("toggleUserActive throws when user not found")
    void toggleUserActive_notFound() {
        when(userRepository.findById(6L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.toggleUserActive(6L));
    }

    @Test
    @DisplayName("saveNewUser encodes password if not starting with $2 or null")
    void saveNewUser_encodesWhenNeeded() {
        User u = new User();
        u.setPassword("plain");
        when(encoder.encode("plain")).thenReturn("$2hsh");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User saved = userService.saveNewUser(u);
        verify(encoder).encode("plain");
        assertEquals("$2hsh", saved.getPassword());
    }

    @Test
    @DisplayName("saveNewUser does not re-encode if password already appears encoded")
    void saveNewUser_skipsEncodeIfAlreadyEncoded() {
        User u = new User();
        u.setPassword("$2already");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User saved = userService.saveNewUser(u);
        verify(encoder, never()).encode(any());
        assertEquals("$2already", saved.getPassword());
    }

    @Test
    @DisplayName("getById returns user when present")
    void getById_found() {
        User u = new User();
        u.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));
        assertSame(u, userService.getById(3L));
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException when absent")
    void getById_notFound() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getById(4L));
    }

    @Test
    @DisplayName("updateProfile throws ResourceNotFoundException when user not found")
    void updateProfile_userNotFound() {
        UpdateUserRequest req = new UpdateUserRequest();
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(10L, req));
    }

    @Test
    @DisplayName("updateProfile throws ConflictException when new email already exists")
    void updateProfile_emailConflict() {
        User u = new User();
        u.setEmail("old@e.com");
        when(userRepository.findById(11L)).thenReturn(Optional.of(u));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("New Name");
        req.setEmail("exists@e.com");
        req.setPhoneNumber("9999999999");

        when(userRepository.existsByEmail("exists@e.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.updateProfile(11L, req));
    }

    @Test
    @DisplayName("updateProfile updates and saves when data is valid")
    void updateProfile_success() {
        User u = new User();
        u.setEmail("old@e.com");
        u.setFullName("Old");
        u.setPhoneNumber("1111111111");
        when(userRepository.findById(12L)).thenReturn(Optional.of(u));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("Updated");
        req.setEmail("new@e.com");
        req.setPhoneNumber("9999999999");

        when(userRepository.existsByEmail("new@e.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.updateProfile(12L, req);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("Updated", saved.getFullName());
        assertEquals("new@e.com", saved.getEmail());
        assertEquals("9999999999", saved.getPhoneNumber());
    }
}


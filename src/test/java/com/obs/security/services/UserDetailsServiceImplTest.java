
package com.obs.security.services;

import com.obs.entity.Role;
import com.obs.entity.User;
import com.obs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @InjectMocks
    private UserDetailsServiceImpl service;

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_success_buildsAuthorities() {
        User user = new User();
        user.setId(100L);
        user.setUsername("neha");
        user.setPassword("secret");
        user.setFullName("Neha Patil");
        user.setEmail("neha@example.com");
        user.setPhoneNumber("9999999999");
        user.setRoles(Set.of(Role.CUSTOMER, Role.BANKER));
        user.setActive(true);

        when(userRepository.findByUsername("neha")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("neha");

        assertEquals("neha", details.getUsername());
        assertTrue(details.isEnabled());
        var auths = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        assertTrue(auths.contains("ROLE_CUSTOMER"));
        assertTrue(auths.contains("ROLE_BANKER"));
    }

    @Test
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
    }
}
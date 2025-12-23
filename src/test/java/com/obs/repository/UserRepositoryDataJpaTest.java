
package com.obs.repository;

import com.obs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryDataJpaTest {

    @Autowired UserRepository userRepository;

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("pw");
        u.setEmail(username + "@example.com");
        u.setPhoneNumber("9999999999");
        u.setFullName("Full " + username);
        return userRepository.save(u);
    }

    @Test
    void findByUsername_returnsUser() {
        newUser("neha");
        Optional<User> found = userRepository.findByUsername("neha");
        assertTrue(found.isPresent());
        assertEquals("neha", found.get().getUsername());
    }

    @Test
    void existsByUsername_and_existsByEmail() {
        newUser("neha");
        assertTrue(userRepository.existsByUsername("neha"));
        assertTrue(userRepository.existsByEmail("neha@example.com"));
        assertFalse(userRepository.existsByUsername("other"));
        assertFalse(userRepository.existsByEmail("other@example.com"));
    }
}


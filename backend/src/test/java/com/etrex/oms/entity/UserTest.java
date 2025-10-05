/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setEmail("test@example.com");
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("password", user.getPassword());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(User.Role.CUSTOMER, user.getRole());
        assertEquals(User.Status.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void testUserDetailsImplementation() {
        assertEquals("testuser", user.getUsername());
        assertEquals("password", user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    void testGetAuthorities_Customer() {
        user.setRole(User.Role.CUSTOMER);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void testGetAuthorities_Admin() {
        user.setRole(User.Role.ADMIN);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testRoleEnum() {
        assertEquals("ADMIN", User.Role.ADMIN.name());
        assertEquals("CUSTOMER", User.Role.CUSTOMER.name());
        assertEquals(User.Role.ADMIN, User.Role.valueOf("ADMIN"));
        assertEquals(User.Role.CUSTOMER, User.Role.valueOf("CUSTOMER"));
    }

    @Test
    void testStatusEnum() {
        assertEquals("ACTIVE", User.Status.ACTIVE.name());
        assertEquals("INACTIVE", User.Status.INACTIVE.name());
        assertEquals(User.Status.ACTIVE, User.Status.valueOf("ACTIVE"));
        assertEquals(User.Status.INACTIVE, User.Status.valueOf("INACTIVE"));
    }

    @Test
    void testInactiveUserNotEnabled() {
        user.setStatus(User.Status.INACTIVE);
        assertFalse(user.isEnabled());
    }

    @Test
    void testActiveUserIsEnabled() {
        user.setStatus(User.Status.ACTIVE);
        assertTrue(user.isEnabled());
    }

    @Test
    void testNoArgsConstructor() {
        User newUser = new User();
        assertNotNull(newUser);
        assertNull(newUser.getId());
    }
}

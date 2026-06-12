package com.libraryforuina.service;

import com.libraryforuina.entity.User;
import com.libraryforuina.enums.Role;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("jan")
                .email("jan@example.com")
                .password("hashed")
                .role(Role.USER)
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: saves new user with encoded password and USER role")
    void register_savesUser() {
        when(userRepository.existsByUsername("jan")).thenReturn(false);
        when(userRepository.existsByEmail("jan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.register("jan", "jan@example.com", "secret");

        assertThat(result.getUsername()).isEqualTo("jan");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws BusinessException when username already taken")
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("jan")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("jan", "jan@example.com", "secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jan");
    }

    @Test
    @DisplayName("register: throws BusinessException when email already in use")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByUsername("jan")).thenReturn(false);
        when(userRepository.existsByEmail("jan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("jan", "jan@example.com", "secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jan@example.com");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns user when found")
    void findById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        User result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById: throws ResourceNotFoundException when not found")
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername: returns user when found")
    void findByUsername_found() {
        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(sampleUser));

        User result = userService.findByUsername("jan");

        assertThat(result.getUsername()).isEqualTo("jan");
    }

    @Test
    @DisplayName("findByUsername: throws ResourceNotFoundException when not found")
    void findByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: returns all users from repository")
    void findAll_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> result = userService.findAll();

        assertThat(result).hasSize(1);
    }

    // ── deleteById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById: deletes existing user")
    void deleteById_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteById: throws ResourceNotFoundException when user not found")
    void deleteById_notFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── promoteToAdmin ────────────────────────────────────────────────────────

    @Test
    @DisplayName("promoteToAdmin: changes role to ADMIN")
    void promoteToAdmin_changesRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        User result = userService.promoteToAdmin(1L);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("promoteToAdmin: throws ResourceNotFoundException when user not found")
    void promoteToAdmin_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.promoteToAdmin(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

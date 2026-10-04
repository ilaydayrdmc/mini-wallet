package com.miniwallet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.miniwallet.wallet.dto.RegisterRequest;
import com.miniwallet.wallet.dto.UserResponse;
import com.miniwallet.wallet.entity.User;
import com.miniwallet.wallet.exception.UsernameTakenException;
import com.miniwallet.wallet.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    // Gercek BCrypt: sifrenin gercekten hash'lendigini dogrulamak icin mock'lanmaz
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void register_storesHashedPasswordAndLowercaseUsername() {
        when(userRepository.existsByUsername("ayse")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = authService.register(new RegisterRequest("Ayse", "gizli-sifre-123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("ayse");
        assertThat(saved.getPasswordHash()).isNotEqualTo("gizli-sifre-123");
        assertThat(passwordEncoder.matches("gizli-sifre-123", saved.getPasswordHash())).isTrue();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("ayse");
    }

    @Test
    void register_existingUsername_throwsAndSavesNothing() {
        when(userRepository.existsByUsername("ayse")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("AYSE", "gizli-sifre-123")))
                .isInstanceOf(UsernameTakenException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_concurrentDuplicate_isReportedAsUsernameTaken() {
        when(userRepository.existsByUsername("ayse")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_username"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("ayse", "gizli-sifre-123")))
                .isInstanceOf(UsernameTakenException.class);
    }
}

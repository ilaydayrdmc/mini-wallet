package com.miniwallet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
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

import com.miniwallet.wallet.dto.LoginRequest;
import com.miniwallet.wallet.dto.RegisterRequest;
import com.miniwallet.wallet.dto.TokenResponse;
import com.miniwallet.wallet.dto.UserResponse;
import com.miniwallet.wallet.entity.User;
import com.miniwallet.wallet.exception.InvalidCredentialsException;
import com.miniwallet.wallet.exception.UsernameTakenException;
import com.miniwallet.wallet.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    // Gercek BCrypt: sifrenin gercekten hash'lendigini dogrulamak icin mock'lanmaz
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, tokenService);
    }

    // ---- giris ----

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User("ayse", passwordEncoder.encode("gizli-sifre-123"));
        TokenResponse token = new TokenResponse("jwt-degeri", "Bearer", 3600);
        when(userRepository.findByUsername("ayse")).thenReturn(Optional.of(user));
        when(tokenService.generateToken(user)).thenReturn(token);

        // Buyuk harf ve bosluk fark etmemeli
        TokenResponse response = authService.login(new LoginRequest(" Ayse ", "gizli-sifre-123"));

        assertThat(response).isSameAs(token);
    }

    @Test
    void login_wrongPassword_throwsAndIssuesNoToken() {
        User user = new User("ayse", passwordEncoder.encode("gizli-sifre-123"));
        when(userRepository.findByUsername("ayse")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ayse", "yanlis-sifre")))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(tokenService);
    }

    @Test
    void login_unknownUser_throwsSameErrorAsWrongPassword() {
        when(userRepository.findByUsername("hayalet")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("hayalet", "gizli-sifre-123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Kullanici adi veya sifre hatali");

        verifyNoInteractions(tokenService);
    }

    // ---- kayit ----

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

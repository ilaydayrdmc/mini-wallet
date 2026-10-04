package com.miniwallet.wallet.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniwallet.wallet.dto.LoginRequest;
import com.miniwallet.wallet.dto.RegisterRequest;
import com.miniwallet.wallet.dto.TokenResponse;
import com.miniwallet.wallet.dto.UserResponse;
import com.miniwallet.wallet.entity.User;
import com.miniwallet.wallet.exception.InvalidCredentialsException;
import com.miniwallet.wallet.exception.UsernameTakenException;
import com.miniwallet.wallet.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Kullanici bulunamadiginda da bir BCrypt karsilastirmasi yapabilmek icin
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // "Ayse" ve "ayse" ayni kullanici sayilir
        String username = normalize(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        try {
            User user = userRepository.saveAndFlush(
                    new User(username, passwordEncoder.encode(request.password())));
            return UserResponse.from(user);
        } catch (DataIntegrityViolationException e) {
            // Ayni anda gelen iki kayit istegi: kontrolu ikisi de gecti, unique kisit yakaladi
            throw new UsernameTakenException(username);
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByUsername(normalize(request.username()));

        // Kullanici olmasa da BCrypt calistirilir: yanit suresinden "bu kullanici var mi"
        // bilgisi sizmasin
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        return tokenService.generateToken(found.get());
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}

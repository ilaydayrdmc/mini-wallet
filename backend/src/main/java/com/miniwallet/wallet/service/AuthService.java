package com.miniwallet.wallet.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniwallet.wallet.dto.RegisterRequest;
import com.miniwallet.wallet.dto.UserResponse;
import com.miniwallet.wallet.entity.User;
import com.miniwallet.wallet.exception.UsernameTakenException;
import com.miniwallet.wallet.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // "Ayse" ve "ayse" ayni kullanici sayilir
        String username = request.username().toLowerCase(Locale.ROOT);
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
}

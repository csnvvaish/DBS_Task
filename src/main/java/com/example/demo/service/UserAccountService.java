package com.example.demo.service;

import com.example.demo.dto.AdminUserResponse;
import com.example.demo.dto.AuthUserResponse;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is too long");
        }
        if (userRepository.existsByUsername(request.username()) || userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already exists");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER,
                LocalDateTime.now());
        return toAuthUser(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(user -> new AdminUserResponse(
                        user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt()))
                .toList();
    }

    @Transactional
    public AuthUserResponse updateRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(role);
        User updatedUser = userRepository.saveAndFlush(user);
        expireSessionsForUser(updatedUser.getId());
        return toAuthUser(updatedUser);
    }

    private void expireSessionsForUser(Long userId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof AuthenticatedUser authenticatedUser
                    && userId.equals(authenticatedUser.getId())) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }

    public AuthUserResponse toAuthUser(User user) {
        return new AuthUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
package com.urlshortener.service;

import com.urlshortener.dto.*;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.exception.BadRequestException;
import com.urlshortener.exception.ResourceNotFoundException;
import com.urlshortener.exception.UnauthorizedException;
import com.urlshortener.repository.RoleRepository;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtRefreshService jwtRefreshService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").build()));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtRefreshService.generateRefreshToken(userDetails);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Invalid email or password"));

            if (!user.getEnabled()) {
                throw new BadRequestException("Account is disabled. Please contact support.");
            }

            if (!user.getAccountNonLocked()) {
                throw new BadRequestException("Account is locked. Please try again later.");
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtRefreshService.generateRefreshToken(userDetails);

            log.info("User logged in: {}", request.getEmail());

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for: {}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtRefreshService.extractUsername(refreshToken);

        if (username == null) {
            throw new BadRequestException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtRefreshService.isTokenValid(refreshToken, userDetails)) {
            throw new BadRequestException("Refresh token has expired. Please login again.");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateToken(userDetails);
        String newRefreshToken = jwtRefreshService.generateRefreshToken(userDetails);

        log.info("Token refreshed for user: {}", username);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, String firstName, String lastName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }

        userRepository.save(user);
        log.info("Profile updated for user: {}", email);

        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", email);
    }

    @Transactional(readOnly = true)
    public void validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new UnauthorizedException("Token is invalid or expired");
            }
        } catch (Exception e) {
            throw new UnauthorizedException("Token validation failed: " + e.getMessage());
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .roles(roles)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .enabled(user.getEnabled())
                .build();
    }
}
package com.bullfitapp.bullfit.user;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegistrationForm form) {
        if (RESERVED.contains(form.getUsername().toLowerCase())) {
            throw new IllegalArgumentException("username");
        }

        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("username");
        }
        String email = (form.getEmail() == null || form.getEmail().isBlank()) ? null : form.getEmail();
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("email");
        }
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setEmail(email);
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        if (form.getHeightFeet() != null || form.getHeightInches() != null) {
            int feet = form.getHeightFeet() == null ? 0 : form.getHeightFeet();
            int inches = form.getHeightInches() == null ? 0 : form.getHeightInches();
            user.setHeight(toInches(form.getHeightFeet(), form.getHeightInches()));
        }
        user.setWeight(form.getWeight());
        user.setBirthDate(form.getBirthDate());
        userRepository.save(user);
    }

    private static final Set<String> RESERVED = Set.of(
            "admin", "developer", "support", "bullfit",
            "settings", "profile", "login", "register", "logout");

    private BigDecimal toInches(Integer feet, Integer inches) {
        if (feet == null && inches == null) return null;
        int total = (feet == null ? 0 : feet) * 12 + (inches == null ? 0 : inches);
        return BigDecimal.valueOf(total);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @Transactional
    public void updateProfile(String username, EditProfileForm form) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setFirstName(blankToNull(form.getFirstName()));
        user.setLastName(blankToNull(form.getLastName()));
        user.setBio(blankToNull(form.getBio()));
        user.setWeight(form.getWeight());
        user.setHeight(toInches(form.getHeightFeet(), form.getHeightInches()));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("currentPassword");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

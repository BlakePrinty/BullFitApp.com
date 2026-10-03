package com.bullfitapp.bullfit.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegistrationForm form) {
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
            user.setHeight(BigDecimal.valueOf(feet * 12 + inches));
        }
        user.setWeight(form.getWeight());
        user.setBirthDate(form.getBirthDate());
        userRepository.save(user);
    }
}

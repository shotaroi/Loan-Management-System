package com.shotaroi.loan.customer;

import com.shotaroi.loan.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(CustomerService customerService, JwtTokenProvider jwtTokenProvider) {
        this.customerService = customerService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        Customer customer = customerService.register(request.email(), request.password());
        log.info("Registration successful for email: {}", customer.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RegisterResponse(customer.getId(), customer.getEmail(), customer.getCreatedAt().toString()));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get access token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Customer customer = customerService.authenticate(request.email(), request.password());
        String accessToken = jwtTokenProvider.createToken(
                customer.getId(), customer.getEmail(), customer.getRole());
        log.info("Login successful for customer: {}", customer.getId());
        return ResponseEntity.ok(new LoginResponse(accessToken));
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password
    ) {}

    public record RegisterResponse(Long customerId, String email, String createdAt) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record LoginResponse(String accessToken) {}
}

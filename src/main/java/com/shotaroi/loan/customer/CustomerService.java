package com.shotaroi.loan.customer;

import com.shotaroi.loan.common.Role;
import com.shotaroi.loan.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer register(String email, String password) {
        if (customerRepository.existsByEmail(email)) {
            throw new ValidationException("Email already registered");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }

        String passwordHash = passwordEncoder.encode(password);
        Customer customer = new Customer(email, passwordHash, Role.USER);
        customer = customerRepository.save(customer);
        log.info("Customer registered: id={}, email={}", customer.getId(), customer.getEmail());
        return customer;
    }

    public Customer authenticate(String email, String password) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return customer;
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new com.shotaroi.loan.common.exception.ResourceNotFoundException("Customer", id));
    }
}

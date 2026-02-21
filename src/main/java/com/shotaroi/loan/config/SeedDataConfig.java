package com.shotaroi.loan.config;

import com.shotaroi.loan.common.Role;
import com.shotaroi.loan.customer.Customer;
import com.shotaroi.loan.customer.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class SeedDataConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataConfig.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataConfig(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin@loan.local", Role.ADMIN);
        seedUser("underwriter@loan.local", Role.UNDERWRITER);
    }

    private void seedUser(String email, Role role) {
        if (customerRepository.existsByEmail(email)) {
            return;
        }
        String hash = passwordEncoder.encode("password123");
        Customer customer = new Customer(email, hash, role);
        customerRepository.save(customer);
        log.info("Seeded {} user: {}", role, email);
    }
}

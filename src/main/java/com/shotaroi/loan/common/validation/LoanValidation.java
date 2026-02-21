package com.shotaroi.loan.common.validation;

import com.shotaroi.loan.common.exception.ValidationException;

import java.math.BigDecimal;

public final class LoanValidation {

    private static final BigDecimal MIN_PRINCIPAL = BigDecimal.ONE;
    private static final int MIN_TERM_MONTHS = 3;
    private static final int MAX_TERM_MONTHS = 360;
    private static final BigDecimal MIN_RATE = BigDecimal.ZERO;
    private static final BigDecimal MAX_RATE = new BigDecimal("0.50");

    private LoanValidation() {}

    public static void validatePrincipal(BigDecimal principal) {
        if (principal == null || principal.compareTo(MIN_PRINCIPAL) < 0) {
            throw new ValidationException("Principal must be greater than 0");
        }
    }

    public static void validateTermMonths(Integer termMonths) {
        if (termMonths == null || termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            throw new ValidationException("Term months must be between 3 and 360");
        }
    }

    public static void validateAnnualInterestRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(MIN_RATE) < 0 || rate.compareTo(MAX_RATE) > 0) {
            throw new ValidationException("Annual interest rate must be between 0 and 0.50 (0% to 50%)");
        }
    }

    public static void validateCurrency(String currency) {
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new ValidationException("Currency must be a valid 3-letter ISO code (e.g. SEK)");
        }
    }

    public static void validateCurrencyMatch(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new ValidationException("Currency mismatch: expected %s, got %s".formatted(expected, actual));
        }
    }

    public static void validatePaymentAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than 0");
        }
    }
}

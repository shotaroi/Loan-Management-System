# Loan Management System

A portfolio-grade Spring Boot REST API for loan applications, underwriting decisions, loan contracts, and repayment management. Built for junior Java developers applying to banks.

## Overview

This system implements core banking concepts:

- **Loan Application**: Customer submits principal, currency, term, and interest rate
- **Underwriting**: Risk assessment with approve/reject decision and reason
- **Loan Contract**: Approved applications become active loans with repayment schedules
- **Amortization**: Fixed monthly installment (annuity) schedule with interest-first allocation
- **Payment Posting**: Payments allocated to accrued interest first, then principal; oldest installments paid first
- **Audit Trail**: Sensitive actions logged for compliance

## Tech Stack

- **Java 21** with **Spring Boot 3.4**
- **PostgreSQL** (Docker)
- **Flyway** for database migrations
- **Spring Security** with JWT (access token)
- **Swagger/OpenAPI** for API documentation
- **BigDecimal** for money (scale 2), **java.time** for dates

## Project Structure

```
src/main/java/com/shotaroi/loan/
├── LoanManagementApplication.java
├── config/
│   ├── AsyncConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── SeedDataConfig.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── SecurityUser.java
├── customer/
│   ├── Customer.java
│   ├── CustomerRepository.java
│   ├── CustomerService.java
│   └── AuthController.java
├── underwriting/
│   ├── LoanApplication.java
│   ├── LoanApplicationRepository.java
│   ├── UnderwritingService.java
│   ├── ApplicationController.java
│   └── UnderwritingController.java
├── loan/
│   ├── Loan.java
│   ├── LoanRepository.java
│   ├── LoanService.java
│   └── LoanController.java
├── schedule/
│   ├── RepaymentSchedule.java
│   ├── RepaymentScheduleRepository.java
│   ├── ScheduleCalculator.java
│   └── ScheduleService.java
├── payment/
│   ├── Payment.java
│   ├── PaymentRepository.java
│   └── PaymentService.java
├── audit/
│   ├── AuditLog.java
│   ├── AuditLogRepository.java
│   ├── AuditService.java
│   └── AuditController.java
└── common/
    ├── Role.java
    ├── ErrorResponse.java
    ├── validation/LoanValidation.java
    └── exception/
        ├── GlobalExceptionHandler.java
        ├── ResourceNotFoundException.java
        ├── ForbiddenException.java
        └── ValidationException.java
```

## How to Run

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

### 3. Seed Users (dev/test)

On startup with `dev` or `test` profile, the following users are created:

| Email                  | Password   | Role        |
|------------------------|------------|-------------|
| admin@loan.local       | password123| ADMIN       |
| underwriter@loan.local | password123| UNDERWRITER |

## Sample cURL Flow

### 1. Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"password123"}'
```

### 2. Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@example.com","password":"password123"}' \
  | jq -r '.accessToken')
```

### 3. Create Application

```bash
APP_ID=$(curl -s -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"principal":100000,"currency":"SEK","termMonths":12,"annualInterestRate":0.05}' \
  | jq -r '.applicationId')
```

### 4. Underwriter Approve (use underwriter token)

```bash
UW_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"underwriter@loan.local","password":"password123"}' \
  | jq -r '.accessToken')

curl -X POST "http://localhost:8080/api/underwriting/applications/$APP_ID/decision" \
  -H "Authorization: Bearer $UW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"decision":"APPROVED","reason":"Good credit"}'
```

### 5. Create Loan

```bash
LOAN_ID=$(curl -s -X POST "http://localhost:8080/api/loans/from-application/$APP_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"startDate\":\"$(date +%Y-%m-%d)\"}" \
  | jq -r '.loanId')
```

### 6. View Schedule

```bash
curl -s "http://localhost:8080/api/loans/$LOAN_ID/schedule?size=20" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 7. Post Payment

```bash
curl -X POST "http://localhost:8080/api/loans/$LOAN_ID/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"amount\":8500,\"currency\":\"SEK\",\"paymentDate\":\"$(date +%Y-%m-%d)\",\"reference\":\"ref-001\"}" | jq
```

## Testing

```bash
mvn test
```

### Unit Tests

- **ScheduleCalculatorTest**: r=0 equal principal; r>0 principal sums to original; last-installment rounding adjustment
- **PaymentAllocationTest**: Interest paid first, then principal; multiple payments mark installments paid in order

### Integration Tests (Testcontainers + PostgreSQL)

- Approve application → create loan → schedule has `termMonths` installments
- Post payment updates outstanding balance and marks installments PAID
- User cannot access another user's loan (403 Forbidden)

## Rounding Strategy

- **Money**: `BigDecimal` with scale 2, `HALF_UP` rounding
- **Schedule**: Monthly payment computed with high precision; last installment principal adjusted so total principal sums exactly to original
- **Payment allocation**: Interest cleared first (accrued + per-installment), then principal; oldest due installments paid first

## Payment Allocation Logic

1. Reject if amount ≤ 0
2. Allocate to **accrued interest** first (loan-level)
3. For each installment (oldest first): pay interest due, then principal due
4. Update `amount_paid` on installments; mark PAID when fully covered
5. If `outstandingPrincipal` = 0 → loan status CLOSED

## Configuration

- `application.yml`: Base config
- `application-dev.yml`: Local PostgreSQL, debug logging
- `application-test.yml`: Testcontainers PostgreSQL

## License

MIT

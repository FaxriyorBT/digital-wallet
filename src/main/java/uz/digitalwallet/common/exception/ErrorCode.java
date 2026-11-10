package uz.digitalwallet.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_FAILED(BAD_REQUEST, "Request validation failed"),
    INVALID_REQUEST(BAD_REQUEST, "Request is invalid"),

    AUTHENTICATION_REQUIRED(UNAUTHORIZED, "Authentication is required"),
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, "Access token is invalid or expired"),
    INVALID_CREDENTIALS(UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "Refresh token is invalid or expired"),
    ACCESS_DENIED(FORBIDDEN, "Access denied"),
    ACCOUNT_BLOCKED(FORBIDDEN, "Account is blocked"),

    USER_NOT_FOUND(NOT_FOUND, "User not found"),
    WALLET_NOT_FOUND(NOT_FOUND, "Wallet not found"),
    TRANSACTION_NOT_FOUND(NOT_FOUND, "Transaction not found"),

    EMAIL_ALREADY_REGISTERED(CONFLICT, "Email is already registered"),
    PHONE_ALREADY_REGISTERED(CONFLICT, "Phone number is already registered"),
    WALLET_ALREADY_EXISTS(CONFLICT, "An open wallet in this currency already exists"),
    IDEMPOTENCY_KEY_IN_USE(CONFLICT, "Another request with this Idempotency-Key is in progress"),
    CONCURRENT_UPDATE(CONFLICT, "The resource was changed by another request, please retry"),
    DATA_CONFLICT(CONFLICT, "The request conflicts with existing data"),

    WRONG_CURRENT_PASSWORD(UNPROCESSABLE_CONTENT, "Current password is incorrect"),
    STATUS_CHANGE_NOT_ALLOWED(UNPROCESSABLE_CONTENT, "This status change is not allowed"),
    WALLET_NOT_EMPTY(UNPROCESSABLE_CONTENT, "Wallet balance must be zero before closing"),
    WALLET_NOT_ACTIVE(UNPROCESSABLE_CONTENT, "Wallet is not active"),
    SAME_WALLET_TRANSFER(UNPROCESSABLE_CONTENT, "Source and target wallets must be different"),
    AMOUNT_OUT_OF_RANGE(UNPROCESSABLE_CONTENT, "Amount is outside the allowed range"),
    IDEMPOTENCY_KEY_REUSED(UNPROCESSABLE_CONTENT, "Idempotency-Key was already used for a different request"),
    RECIPIENT_UNAVAILABLE(UNPROCESSABLE_CONTENT, "Recipient wallet cannot accept transfers"),
    INSUFFICIENT_FUNDS(UNPROCESSABLE_CONTENT, "Insufficient funds"),
    DAILY_LIMIT_EXCEEDED(UNPROCESSABLE_CONTENT, "Daily transfer limit exceeded"),

    TOO_MANY_LOGIN_ATTEMPTS(TOO_MANY_REQUESTS, "Too many failed login attempts, try again later"),
    EXCHANGE_RATE_UNAVAILABLE(SERVICE_UNAVAILABLE, "Exchange rate is not available at the moment"),
    INTERNAL_ERROR(INTERNAL_SERVER_ERROR, "Unexpected error, please try again later");

    private final HttpStatus status;
    private final String message;
}

package uz.digitalwallet.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uzbekistan mobile number in international format, e.g. +998901234567.
 * {@code null} is considered valid, combine with {@code @NotBlank} when the value is required.
 */
@Pattern(regexp = "^\\+998\\d{9}$", message = "must be in the format +998XXXXXXXXX")
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "is not a valid phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package uz.digitalwallet.exchange.dto;

import uz.digitalwallet.common.money.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(Currency currency, BigDecimal rate, LocalDate date) {
}

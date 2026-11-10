package uz.digitalwallet.admin.dto;

import uz.digitalwallet.transaction.CurrencyTurnover;

import java.time.LocalDate;
import java.util.List;

public record DailyStatsResponse(LocalDate date, List<CurrencyTurnover> turnover) {
}

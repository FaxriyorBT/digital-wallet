package uz.digitalwallet.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * One item of the Central Bank of Uzbekistan rates feed, e.g.
 * {"Ccy":"USD","Nominal":"1","Rate":"11987.16","Date":"07.11.2026",...}. All values arrive as strings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CbuRate(
        @JsonProperty("Ccy") String currencyCode,
        @JsonProperty("Nominal") String nominal,
        @JsonProperty("Rate") String rate,
        @JsonProperty("Date") String date) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int RATE_SCALE = 4;

    /**
     * Some currencies are quoted per 10 or 100 units, so the rate is divided by the nominal.
     */
    public BigDecimal ratePerUnit() {
        return new BigDecimal(rate).divide(new BigDecimal(nominal), RATE_SCALE, RoundingMode.HALF_EVEN);
    }

    public LocalDate rateDate() {
        return LocalDate.parse(date, DATE_FORMAT);
    }
}

package uz.digitalwallet.exchange;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.digitalwallet.exchange.dto.ExchangeRateResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange rates")
@SecurityRequirements
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "Latest official CBU rates: price of one unit of each currency in UZS")
    public List<ExchangeRateResponse> latestRates() {
        return exchangeRateService.latestRates();
    }
}

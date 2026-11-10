package uz.digitalwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DigitalWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalWalletApplication.class, args);
    }
}

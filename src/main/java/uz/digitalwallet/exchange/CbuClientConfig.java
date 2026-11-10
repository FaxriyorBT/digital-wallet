package uz.digitalwallet.exchange;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration(proxyBeanMethods = false)
class CbuClientConfig {

    /**
     * Without explicit timeouts a hanging external server would block our thread for as long as the OS allows.
     */
    @Bean
    RestClient cbuRestClient(RestClient.Builder builder, ExchangeProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.cbuUrl())
                .requestFactory(requestFactory)
                .build();
    }
}

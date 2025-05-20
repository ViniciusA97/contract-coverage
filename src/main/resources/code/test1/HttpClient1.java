package instrument.test1;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient1 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void doRequest() {
        // URL -> CtLiteral
        // Method -> CtFieldReadImpl
        // null -> CtLiteralImpl
        // String.class -> CtFieldReadImpl
        restTemplate.exchange("http://localhost:8080", HttpMethod.GET, null, String.class);
    }
}
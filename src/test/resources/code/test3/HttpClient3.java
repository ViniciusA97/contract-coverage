package instrument.test3;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * This class is used to represent a simple HTTP Client.
 */
public class HttpClient3 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void doRequest(String url, HttpMethod method) {
        restTemplate.exchange(url, method, null, String.class);
    }
}


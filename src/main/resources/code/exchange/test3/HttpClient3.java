package instrument.test3;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient3 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void doRequest(String url, HttpMethod method) {
        restTemplate.exchange(url, method, null, String.class);
    }
}


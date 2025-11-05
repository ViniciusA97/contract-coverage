package instrument.test4;

import org.springframework.web.client.RestTemplate;

public class HttpClient4 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        restTemplate.postForObject("http://localhost:8080/test-4", null, String.class);
    }
}



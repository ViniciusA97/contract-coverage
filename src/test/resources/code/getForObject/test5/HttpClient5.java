package instrument.test5;

import org.springframework.web.client.RestTemplate;

public class HttpClient5 {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8080";

    public void get() {
        this.doRequest();
    }

    private void doRequest() {
        restTemplate.getForObject(baseUrl + "/test-5", String.class);
    }
}



package instrument.test5;

import org.springframework.web.client.RestTemplate;

public class HttpClient5 {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8080";

    public void post() {
        this.doRequest();
    }

    private void doRequest() {
        restTemplate.postForObject(baseUrl + "/test-5", null, String.class);
    }
}



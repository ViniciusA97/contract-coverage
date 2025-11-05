package instrument.test6;

import org.springframework.web.client.RestTemplate;

public class HttpClient6 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest();
    }

    private void doRequest() {
        String url = "http://localhost:8080/test-6";
        restTemplate.postForObject(url, null, String.class);
    }
}



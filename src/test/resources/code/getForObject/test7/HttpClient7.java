package instrument.test7;

import org.springframework.web.client.RestTemplate;

public class HttpClient7 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void get(String variable) {
        this.doRequest("/variable", variable);
    }

    private void doRequest(String path, String variable) {
        String url = "http://localhost:8080" + path + "/" + variable;
        restTemplate.getForObject(url, String.class);
    }
}



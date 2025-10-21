package instrument.test6;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient6 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest();
    }
    private void doRequest() {
        String url = "http://localhost:8080/test-2" ;
        HttpMethod method = HttpMethod.POST;
        restTemplate.exchange(url, method, null, String.class);
    }
}


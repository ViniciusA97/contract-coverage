package instrument.test10;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient10 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.callInternal();
    }

    private void callInternal() {
        String url = "http://localhost:8080/local-vars";
        HttpMethod method = HttpMethod.POST;
        restTemplate.exchange(url, method, null, String.class);
    }
}

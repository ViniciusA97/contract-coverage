package instrument.test9;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient9 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.wrapperLevel1("http://localhost:8080/multi-wrapper", HttpMethod.POST);
    }

    private void wrapperLevel1(String url, HttpMethod method) {
        wrapperLevel2(url, method);
    }

    private void wrapperLevel2(String finalUrl, HttpMethod finalMethod) {
        restTemplate.exchange(finalUrl, finalMethod, null, String.class);
    }
}

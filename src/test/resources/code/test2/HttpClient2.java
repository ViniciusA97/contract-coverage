package instrument.test2;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * This class is used to represent a simple HTTP Client. In our test, this API call need to be registred with a
 * Endpoint(path="/test-2", method="POST") because is called in another point of the code (in the same client).
 */
public class HttpClient2 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest("http://localhost:8080/test-2", HttpMethod.POST);
    }

    private void doRequest(String finalUrl, HttpMethod method) {
        restTemplate.exchange(finalUrl, method, null, String.class);
    }
}


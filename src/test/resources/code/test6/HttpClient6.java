package instrument.test6;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * This class is used to represent a object that call a Client Wrapper. In our test, this API call need to be registred
 * as Endpoint(path="/test-3", metbod="POST").
 */
public class HttpClient6 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void post() {
        this.doRequest();
    }
    private void doRequest() {
        String url = "http://localhost:8080/test-6" ;
        HttpMethod method = HttpMethod.POST;
        restTemplate.exchange(url, method, null, String.class);
    }
}


package instrument.test1;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;


/**
 * This class is used to represent a simple HTTP Client. In our test, this API call need to be ignored because is not
 * called in another point of the code.
 */
public class HttpClient1 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void doRequest() {
        restTemplate.exchange("http://localhost:8080", HttpMethod.GET, null, String.class);
    }
}
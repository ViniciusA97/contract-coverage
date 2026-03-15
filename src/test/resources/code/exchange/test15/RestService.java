package instrument.test15;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * This class simulates a RestService that creates RestTemplate inside a method (not as a field).
 * Pattern: restTemplate().exchange(...)
 * 
 * This is the pattern used by GitHub and Everhour RestService classes.
 */
public class RestService {
    
    public <T> ResponseEntity<T> performGet(URI resourceURI, Class<T> responseType) {
        HttpEntity<String> request = new HttpEntity<>(null);
        return restTemplate().exchange(resourceURI, HttpMethod.GET, request, responseType);
    }
    
    private RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(java.time.Duration.ofMillis(15000))
                .setReadTimeout(java.time.Duration.ofMillis(20000))
                .build();
    }
}

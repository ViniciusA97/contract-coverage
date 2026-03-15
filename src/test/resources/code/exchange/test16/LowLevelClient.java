package instrument.test16;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * This class simulates a low-level HTTP client wrapper like CleverReachLowLevelClient.
 * It creates RestTemplate via a method (not a field) and has generic HTTP methods
 * that receive the URL/path as a parameter.
 * 
 * Pattern: createRestTemplate().exchange(url, ...) where url is a method parameter.
 */
public class LowLevelClient {
    
    private static final String BASE_URL = "https://api.example.com/v2";
    
    public <T> T get(String urlPathAndParams, Class<T> responseType) {
        RestTemplate restTemplate = createRestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        
        ResponseEntity<T> response = restTemplate.exchange(
                urlPathAndParams,
                HttpMethod.GET,
                entity,
                responseType);
        return response.getBody();
    }
    
    public <I, O> O post(I requestBody, String url, Class<O> responseType) {
        RestTemplate restTemplate = createRestTemplate();
        HttpEntity<I> entity = new HttpEntity<>(requestBody, createHeaders());
        
        ResponseEntity<O> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                responseType);
        return response.getBody();
    }
    
    public <I, O> O put(I requestBody, String url, Class<O> responseType) {
        RestTemplate restTemplate = createRestTemplate();
        HttpEntity<I> entity = new HttpEntity<>(requestBody, createHeaders());
        
        ResponseEntity<O> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                responseType);
        return response.getBody();
    }
    
    public void delete(String url) {
        RestTemplate restTemplate = createRestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }
    
    private RestTemplate createRestTemplate() {
        return new RestTemplateBuilder().rootUri(BASE_URL).build();
    }
    
    private HttpHeaders createHeaders() {
        return new HttpHeaders();
    }
}

package instrument.test7;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient7 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void put() {
        this.doRequest("http://localhost:8080/variable/test7", HttpMethod.PUT);
    }

    // url -> CtVariableReadImpl
    // method -> CtVariableReadImpl
    // null -> CtLiteralImpl
    // String.class -> CtFieldReadImpl
    private void doRequest(String url, HttpMethod method) {
        restTemplate.put(url, null);
    }
}

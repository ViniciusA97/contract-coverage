package instrument.test5;

import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class HttpClient5 {
    private final RestTemplate restTemplate = new RestTemplate();

    public void get() {
        this.doRequest("http://localhost:8080/test-5", HttpMethod.GET);
    }

    // url -> CtVariableReadImpl
    // method -> CtVariableReadImpl
    // null -> CtLiteralImpl
    // String.class -> CtFieldReadImpl
    private void doRequest(String url, HttpMethod method) {
        restTemplate.getForEntity(url, String.class);
    }
}

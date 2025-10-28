package instrument.test3;

import org.springframework.http.HttpMethod;

/**
 * This class is used to represent a object that call a Client Wrapper. In our test, this API call need to be registred
 * as Endpoint(path="/test-3", metbod="POST").
 */
public class Main {
    public static void main(String[] args) {
        HttpClient3 httpClient = new HttpClient3();
        httpClient.doRequest("http://localhost:8080/test-3", HttpMethod.POST);
    }
}

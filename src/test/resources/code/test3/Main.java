package instrument.test3;

import org.springframework.http.HttpMethod;

public class Main {
    public static void main(String[] args) {
        HttpClient3 httpClient = new HttpClient3();
        httpClient.doRequest("http://localhost:8080/test-3", HttpMethod.POST);
    }
}

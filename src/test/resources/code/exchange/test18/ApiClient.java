package instrument.test18;

public class ApiClient {
    private final RestService restService;
    
    public ApiClient(RestService restService) {
        this.restService = restService;
    }
    
    public Object[] getUsers() {
        return restService.performGet("/users", Object[].class).getBody();
    }
    
    public Object getUser(String id) {
        return restService.performGet("/users/" + id, Object.class).getBody();
    }
}

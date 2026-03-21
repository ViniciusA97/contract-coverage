package instrument.test18;

public class Main {
    public static void main(String[] args) {
        RestService restService = new RestService();
        ApiClient client = new ApiClient(restService);
        client.getUsers();
        // Pass a dynamic value (args[0]) to test dynamic resolution
        client.getUser(args[0]);
    }
}

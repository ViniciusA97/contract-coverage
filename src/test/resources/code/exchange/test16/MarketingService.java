package instrument.test16;

/**
 * Service that uses the LowLevelClient wrapper.
 * Calls the wrapper methods with specific endpoint paths.
 */
public class MarketingService {
    
    private static final String GROUPS_ENDPOINT = "/groups";
    private static final String RECEIVERS_ENDPOINT = "/receivers";
    
    private final LowLevelClient client;
    
    public MarketingService(LowLevelClient client) {
        this.client = client;
    }
    
    public String getGroups() {
        return client.get(GROUPS_ENDPOINT, String.class);
    }
    
    public String createReceiver(Object receiver) {
        return client.post(receiver, RECEIVERS_ENDPOINT, String.class);
    }
    
    public String updateReceiver(Object receiver) {
        return client.put(receiver, RECEIVERS_ENDPOINT, String.class);
    }
    
    public void deleteReceiver() {
        client.delete(RECEIVERS_ENDPOINT);
    }
}

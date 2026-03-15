package instrument.test16;

public class Main {
    public static void main(String[] args) {
        LowLevelClient client = new LowLevelClient();
        MarketingService service = new MarketingService(client);
        
        service.getGroups();
        service.createReceiver(new Object());
        service.updateReceiver(new Object());
        service.deleteReceiver();
    }
}

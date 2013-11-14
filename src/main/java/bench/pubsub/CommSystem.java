package bench.pubsub;

public interface CommSystem {
    public void init(MessageProcessor msgProc);
    public abstract void sendMessage(byte[] data);
}

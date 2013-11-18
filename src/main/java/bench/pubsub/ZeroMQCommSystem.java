package bench.pubsub;

import org.zeromq.ZMQ;

public class ZeroMQCommSystem implements CommSystem {

    private ZMQ.Context context;
    private ZMQ.Socket subscriber;
    private ZMQ.Socket request;
    

    public void init(final MessageProcessor msgProc) {
        //  Prepare our context and publisher
        context = ZMQ.context(1);

        subscriber = context.socket(ZMQ.SUB);
        subscriber.connect("tcp://localhost:5556");
        subscriber.subscribe(new byte[0]);

        request = context.socket(ZMQ.PUSH);
        request.connect("tcp://localhost:5557");
        
        Thread t = new Thread() {
            public void run() {
                while (! Thread.currentThread().isInterrupted()) {
                    byte[] data = subscriber.recv(0);
                    //System.out.printf("SUB: Received message with %d bytes\n", data.length);

                    CustomMessage msg = MessageSender.makeCustomMessage(data);
                    msgProc.process(msg);
                }
            }
        };
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

        // HACK!  Just to see if the connection to the publisher happens before continuing...
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            // nop
        }
    }

    public void sendMessage(byte[] data) {
        //System.out.printf("I'm sending a message with %d bytes\n", msg.data.length);
        //request.send(data, 0);
        request.send(data, ZMQ.DONTWAIT);
        //System.out.println("DONE");
    }
}

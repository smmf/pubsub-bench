package bench.pubsub;

import org.zeromq.ZMQ;

/**
 * This sequencer simply reads from a PULL socket and forwards the
 * message to the PUB socket.
 */

public class ZeroMQSequencer {
    
    public static void main (String[] args) throws Exception {
        new Thread(new Sequencer()).start();
    }

    public static class Sequencer implements Runnable {
        public void run() {
            //  Prepare our context and publisher
            ZMQ.Context context = ZMQ.context(2);
            

            ZMQ.Socket publisher = context.socket(ZMQ.PUB);
            publisher.setAffinity(1);
            //publisher.setSndHWM(100000);
            publisher.bind("tcp://*:5556");
            //publisher.bind("ipc://weather");

            ZMQ.Socket incoming = context.socket(ZMQ.PULL);
            //incoming.setRcvHWM(100000);
            incoming.setAffinity(2);
            incoming.bind("tcp://*:5557");

            ZMQ.proxy(incoming, publisher, null);
            // while (! Thread.currentThread().isInterrupted()) {
            //     byte[] data = incoming.recv(0);
            //     //System.out.printf("Received message with %d bytes\n", data.length);
            //     publisher.send(data, 0);
            // }

            publisher.close();
            incoming.close();
            context.term();
        }
    }
}

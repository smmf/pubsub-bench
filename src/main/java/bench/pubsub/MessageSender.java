package bench.pubsub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.util.concurrent.LinkedBlockingQueue;

public class MessageSender implements Serializable {
    private final LinkedBlockingQueue messages = new LinkedBlockingQueue();
    private CommSystem commSystem;

    public MessageSender(CommSystem commSystem) {
        this.commSystem = commSystem;
    }

    public void start() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        //System.out.println("Waiting for something in the queue...");
                        byte[] data = (byte[])messages.take();
                        //System.out.println("Found something in the queue...");
                        commSystem.sendMessage(data);
                        //System.out.println("Sent to commSystem...");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }


    public void sendMessage(CustomMessage msg) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(msg.estimateSizeInBytes());
        try {
            DataOutputStream dos = new DataOutputStream(baos);
            msg.writeToDataOutput(dos);
            dos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        byte[] data = baos.toByteArray();
        messages.add(data);
    }

    public static CustomMessage makeCustomMessage(byte[] data) {
        // rebuild CustomMessage from data
        CustomMessage msg = new CustomMessage();
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            msg.readFromDataInput(dis);
            dis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return msg;
    }
}

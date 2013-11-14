package bench.pubsub;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bench {

    private static final Logger logger = LoggerFactory.getLogger(Bench.class);

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        System.setProperty("current.date", dateFormat.format(new Date()));
    }

<<<<<<< HEAD
    public static final String HZL_CONFIG_FILE = "hazelcast.xml";
    public static final String HZL_GROUP_NAME = "PubSubGroup";
    public static final String HZL_TOPIC_NAME = "PubSubTopic";

    public static HazelcastInstance HAZELCAST_INSTANCE;
=======
    public static MessageSender msgSender;
    
>>>>>>> Support ZeroMQ as CommSystem.

    public static final AtomicReference<CustomMessage> incomingMessagesHead = new AtomicReference<CustomMessage>();

    public static CustomMessage incomingMessagesTail; // updated only by the delivery (single) thread

    public static int nThreads;
    public static int payload; // # of bytes to send in the custom message
    private static boolean startSequencer = false;
    private static String commSystemName = HazelcastCommSystem.class.getSimpleName();

    public static void main(String[] args) throws Exception {
        logger.info("process arguments");
        processArgs(args);

        logger.info("booting the communication system");
        bootCommSystem();

        logger.info("spawn worker threads");
        startWork();
    }

    private static void processArgs(String[] args) {
        if (args.length > 0) {
            commSystemName = args[0];
            if (args.length > 1) {
                nThreads = Integer.parseInt(args[1]);
                if (args.length > 2) {
                    payload = Integer.parseInt(args[2]);
                    if (args.length > 3) {
                        startSequencer = true;
                    }
                } else {
                    payload = 100;
                }
            } else {
                nThreads = 1;
                payload = 100;
            }
        }
        logger.info("Running with nThreads={}, payload={}", nThreads, payload);
    }

    private static void bootCommSystem() throws Exception {
        incomingMessagesHead.set(CustomMessage.makeSentinel());
        incomingMessagesTail = getMessageAtHead();
        
        String csFullName = "bench.pubsub." + commSystemName;
        logger.info("Using {}", csFullName);
        
        CommSystem commSystem = null;
        try {
            commSystem = (CommSystem)Class.forName(csFullName).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        if (startSequencer) {
            startSequencer();
        }
        
        commSystem.init(new MessageProcessor() {
                @Override
                public void process(CustomMessage m) {
                    CustomMessage currentTail = incomingMessagesTail;
                    logger.trace("currentTail={} (before enqueue)", incomingMessagesTail.getId());

                    // this should be running on a single thread, so this CAS should never fail
                    if (!currentTail.setNext(m)) {
                        enqueueFailed();
                    }

                    // update last known tail
                    incomingMessagesTail = m;
                }
                
                private void enqueueFailed() throws AssertionError {
                    String message = "Impossible condition: failed to enqueue commit request";
                    logger.error(message);
                    throw new AssertionError(message);
                }
            });

        msgSender = new MessageSender(commSystem);
        msgSender.start();
    }

    public static CustomMessage getMessageAtHead() {
        return incomingMessagesHead.get();
    }

    public static void sendMessage(CustomMessage message) {
        msgSender.sendMessage(message);
    }

    /**
     * Clears the given message from the head of the incoming queue if: (1) there is a next one; AND (2) the head is
     * still the given message.
     * 
     * @param message The message to remove from the head
     * @return The message left at the head. This can be either: (1) The message given as argument (if there is no next message
     *         yet); (2) the message following the one given in the argument, otherwise.
     */
    public static CustomMessage tryToRemoveMessage(CustomMessage message) {
        CustomMessage next = message.getNext();

        if (next == null) {
            logger.trace("Message {} has no next yet.  Must remain at the head", message.getId());
            return message;
        }

        if (incomingMessagesHead.compareAndSet(message, next)) {
            logger.trace("Removed message {} from the head.", message.getId());
        } else {
            logger.trace("Message {} was no longer at the head.", message.getId());
        }
        return next;
    }

    private static void startWork() {
        for (int i = 0; i < nThreads; i++) {
            new Thread(new PubSubWorker()).start();
        }
    }

    private static void startSequencer() {
        new Thread(new ZeroMQSequencer.Sequencer()).start();
    }
}

package bench.pubsub;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class Bench {

    private static final Logger logger = LoggerFactory.getLogger(Bench.class);

    public static final String HZL_CONFIG_FILE = "hazelcast.xml";
    public static final String HZL_GROUP_NAME = "PubSubGroup";
    public static final String HZL_TOPIC_NAME = "PubSubTopic";

    public static HazelcastInstance HAZELCAST_INSTANCE;

    public static final ConcurrentLinkedQueue<CustomMessage> receiveQueue = new ConcurrentLinkedQueue<>();

    public static final AtomicReference<CustomMessage> incomingMessagesHead = new AtomicReference<CustomMessage>();

    public static CustomMessage incomingMessagesTail; // updated only by the delivery (single) thread

    public static int nThreads;
    public static int payload; // # of bytes to send in the custom message

    public static void main(String[] args) {
        logger.info("process arguments");
        processArgs(args);

        logger.info("booting the communication system");
        bootCommSystem();

        logger.info("register listener for incoming messages");
        registerTopicListener();

        logger.info("spawn worker threads");
        startWork();
    }

    private static void processArgs(String[] args) {
        if (args.length > 0) {
            nThreads = Integer.parseInt(args[0]);
            if (args.length > 1) {
                payload = Integer.parseInt(args[1]);
            } else {
                payload = 100;
            }
        } else {
            nThreads = 1;
            payload = 100;
        }
        logger.info("Running with nThreads={}, payload={}", nThreads, payload);
    }

    private static void bootCommSystem() {
        incomingMessagesHead.set(CustomMessage.makeSentinel());
        incomingMessagesTail = getMessageAtHead();

        com.hazelcast.config.Config hzlCfg = getHazelcastConfig();
        HAZELCAST_INSTANCE = Hazelcast.newHazelcastInstance(hzlCfg);
    }

    private static void registerTopicListener() {
        ITopic<CustomMessage> topic = HAZELCAST_INSTANCE.getTopic(HZL_TOPIC_NAME);

        topic.addMessageListener(new MessageListener<CustomMessage>() {
            @Override
            public void onMessage(Message<CustomMessage> message) {
                CustomMessage m = message.getMessageObject();

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
    }

    static Config getHazelcastConfig() {
        System.setProperty("hazelcast.logging.type", "slf4j");
        com.hazelcast.config.Config hzlCfg = new ClasspathXmlConfig(HZL_CONFIG_FILE);
        hzlCfg.getGroupConfig().setName(HZL_GROUP_NAME);

        // turn on global ordering for the topic
        TopicConfig topicConfig = hzlCfg.getTopicConfig(HZL_TOPIC_NAME);
        topicConfig.setGlobalOrderingEnabled(true);
        hzlCfg.addTopicConfig(topicConfig);

        topicConfig = hzlCfg.getTopicConfig(HZL_TOPIC_NAME);

        return hzlCfg;
    }

    public static CustomMessage getMessageAtHead() {
        return incomingMessagesHead.get();
    }

    public static void sendMessage(CustomMessage message) {
        ITopic<CustomMessage> topic = HAZELCAST_INSTANCE.getTopic(HZL_TOPIC_NAME);
        topic.publish(message);
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

}

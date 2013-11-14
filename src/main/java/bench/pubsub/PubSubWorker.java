package bench.pubsub;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubSubWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PubSubWorker.class);

    private static final long STATS_DUMP_INTERVAL = 3_000_000_000L; // in nanoseconds

    private int totalMsgCount = 0;
    private long totalLatency = 0;
    private int totalIterationsPerMessage;

    private int intervalMsgCount = 0;
    private long intervalLatency = 0;
    private int intervalIterationsPerMessage;

    private long lastStatsDump = System.nanoTime();

    @Override
    public void run() {
        while (true) {
            totalMsgCount++;
            intervalMsgCount++;

            logger.trace("Read the current head of the incoming messages");
            CustomMessage currentHead = Bench.getMessageAtHead();
            logger.debug("Message at head is {}", currentHead.getId());

            logger.trace("Create a message");
            CustomMessage message = new CustomMessage(Bench.payload);
            UUID myMessageId = message.getId();

            logger.trace("Broadcast my message");
            long startTime = System.nanoTime();         // measure time
            sendMessage(message);

            logger.trace("Iterate messages until my message appears");
            int iterationsPerMessage = waitForMyMessage(currentHead, myMessageId);

            long endTime = System.nanoTime();              // measure time

            long latency = endTime - startTime;
            logger.debug("Latency: {}", latency);

            // store statistics
            totalLatency += latency;
            totalIterationsPerMessage += iterationsPerMessage;
            intervalLatency += latency;
            intervalIterationsPerMessage += iterationsPerMessage;

            // occasionally dump them
            if (endTime - lastStatsDump > STATS_DUMP_INTERVAL) {
                logger.info(
                        "Totals: messages={}, avg latency={}, avg iterations per message={}. Partials: messages={}, avg latency={}, avg iterations per message={}.",
                        totalMsgCount, totalLatency / totalMsgCount, totalIterationsPerMessage / totalMsgCount, intervalMsgCount,
                        intervalLatency / intervalMsgCount, intervalIterationsPerMessage / intervalMsgCount);

                intervalLatency = 0;
                intervalIterationsPerMessage = 0;
                intervalMsgCount = 0;
                lastStatsDump = endTime;
            }
        }
    }

    private UUID sendMessage(CustomMessage message) {
        Bench.sendMessage(message);
        return message.getId();
    }

    private int waitForMyMessage(CustomMessage currentHead, UUID myMessageId) {
        CustomMessage current = currentHead;

        int pos = 0;

        do {
            pos++;
            current = waitForNextMessage(current);

            if (current.getId().equals(myMessageId)) {
                logger.debug("Found my message after {} iterations.", pos);
                return pos;
            }
        } while (true);
    }

    protected static CustomMessage waitForNextMessage(CustomMessage lastSeenMessage) {
        CustomMessage next = null;
        while ((next = lastSeenMessage.getNext()) == null) {
            // empty
        }

        Bench.tryToRemoveMessage(lastSeenMessage);

        return next;
    }

}

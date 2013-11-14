package bench.pubsub;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;


public class HazelcastCommSystem implements CommSystem {

    public static final String HZL_CONFIG_FILE = "hazelcast.xml";
    public static final String HZL_GROUP_NAME = "PubSubGroup";
    public static final String HZL_TOPIC_NAME = "PubSubTopic";

    private HazelcastInstance hazelcastInstance;

    public void init(final MessageProcessor msgProc) {
        com.hazelcast.config.Config hzlCfg = getHazelcastConfig();
        hazelcastInstance = Hazelcast.newHazelcastInstance(hzlCfg);

        ITopic topic = hazelcastInstance.getTopic(HZL_TOPIC_NAME);

        topic.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                CustomMessage m = MessageSender.makeCustomMessage((byte[])message.getMessageObject());

                msgProc.process(m);
            }
        });
    }

    Config getHazelcastConfig() {
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

    public void sendMessage(byte[] data) {
        ITopic topic = hazelcastInstance.getTopic(HZL_TOPIC_NAME);
        topic.publish(data);
    }
}

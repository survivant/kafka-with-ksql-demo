package com.example.kafkaeventalarm.manager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Service;

import com.example.kafkaeventalarm.model.Order;

@Service
public class OrderManager {
    private final Logger logger = LoggerFactory.getLogger(OrderManager.class);

    @Value(value = "${order.topic.name}")
    private String orderTopic;

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Value(value = "${order.topic.group.id}")
    private String orderGroupId;

    @Value(value = "${return.topic.group.id}")
    private String returnGroupId;

    public ConsumerFactory<String, Order> orderReplayConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "OrderConsumerReplay" + Instant.now().toString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, orderGroupId + "_replay_" + Instant.now().toString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    public List<Order> consumeHistory() {
        List<Order> orders = new ArrayList<>();

        var consumer = (KafkaConsumer) orderReplayConsumerFactory().createConsumer();

        consumer.subscribe(Arrays.asList(orderTopic));

        while (true) {
            ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(1000));
            if (records.isEmpty()) {
                break;
            }
            logger.info("total records = {}", records.count());
            for (ConsumerRecord<String, Order> record : records) {
                logger.info(record.toString());
                orders.add(record.value());
            }
        }
        consumer.close();

        return orders;
    }

    /**
     * Returns the partitions for the provided topics
     *
     * @param topics collection of Kafka topics
     * @return the partitions for the provided topics
     * @throws org.apache.kafka.common.KafkaException if there is an issue fetching the partitions
     * @throws IllegalArgumentException               if topics is null
     */
    public Collection<TopicPartition> getPartitionsFor(KafkaConsumer consumer, Collection<String> topics) {
        if (topics == null)
            throw new IllegalArgumentException("topics cannot be null");
        return topics.stream()
                .flatMap(topic -> {
                    List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);

                    // partition infos could be null if the topic does not exist
                    if (partitionInfos == null)
                        return Collections.<TopicPartition>emptyList().stream();

                    return partitionInfos.stream()
                            .map(partitionInfo -> new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
                })
                .collect(Collectors.toList());

    }

    public Map<TopicPartition, Long> getOffsetsForTimes(KafkaConsumer consumer, Collection<String> topics, long time) {
        if (topics == null)
            throw new IllegalArgumentException("topics cannot be null");

        Collection<TopicPartition> partitions = getPartitionsFor(consumer, topics);

        //Find all the offsets at a specified time.
        var topicTimes = getPartitionsFor(consumer, topics).stream().collect(Collectors.toMap(Function.identity(), s -> time));
        Map<TopicPartition, OffsetAndTimestamp> foundOffsets = consumer.offsetsForTimes(topicTimes);

        //merge the offsets together into a single collection.
        Map<TopicPartition, Long> offsets = new HashMap<>();
        offsets.putAll(foundOffsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset())));

        //if some partitions do not have offsets at the specified time, find the earliest partitions for that time.
        var missingPartitions = partitions.stream()
                .filter(t -> !foundOffsets.containsKey(t)).collect(Collectors.toList());
        if (!missingPartitions.isEmpty()) {
            Map<TopicPartition, Long> missingOffsets = consumer.endOffsets(missingPartitions);
            offsets.putAll(missingOffsets);
        }

        return offsets;
    }

    public List<Order> consumeHistory(String timestamp) {
        List<Order> orders = new ArrayList<>();

        var consumer = (KafkaConsumer) orderReplayConsumerFactory().createConsumer();

        var startTimestamp = 1619100504712L;

        var seekToTimeOnRebalance = new SeekToTimeOnRebalance(consumer, startTimestamp);

        // subscribe to the input topic and listen for assignments.
        consumer.subscribe(Arrays.asList(orderTopic), seekToTimeOnRebalance);

        // poll and process the records.
        ConsumerRecords<String, Order> records = consumer.poll(Duration.ofMillis(1000));
        for (ConsumerRecord<String, Order> record : records) {
            // The offsetsForTimes API returns the earliest offset in a topic-partition with a timestamp
            // greater than or equal to the input timestamp. There could be messages following that offset
            // with timestamps lesser than the input timestamp. Let's skip such messages.
            if (record.timestamp() < startTimestamp) {
                System.out.println("Skipping out of order record with key " + record.key() + " timestamp " + record.timestamp());
                continue;
            }
            System.out.println("record key " + record.key() + "record timestamp " + record.timestamp() + "record offset " + record.offset());

            orders.add(record.value());

        }
        consumer.close();

        return orders;
    }

    public List<Order> getAllOrders() {
        return consumeHistory();
    }

    public List<Order> getAllOrders(String timestamp) {
        return consumeHistory(timestamp);
    }

    class SeekToTimeOnRebalance implements ConsumerRebalanceListener {
        private final Long startTimestamp;
        private Consumer<?, ?> consumer;

        public SeekToTimeOnRebalance(Consumer<?, ?> consumer, Long startTimestamp) {
            this.consumer = consumer;
            this.startTimestamp = startTimestamp;
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
            for (TopicPartition partition : partitions) {
                timestampsToSearch.put(partition, startTimestamp);
            }
            // for each assigned partition, find the earliest offset in that partition with a timestamp
            // greater than or equal to the input timestamp
            var outOffsets = consumer.offsetsForTimes(timestampsToSearch);
            for (TopicPartition partition : partitions) {
                Long seekOffset = outOffsets.get(partition).offset();
                Long currentPosition = consumer.position(partition);
                // seek to the offset returned by the offsetsForTimes API
                // if it is beyond the current position
                if (seekOffset.compareTo(currentPosition) > 0) {
                    consumer.seek(partition, seekOffset);
                }
            }
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        }

    }
}

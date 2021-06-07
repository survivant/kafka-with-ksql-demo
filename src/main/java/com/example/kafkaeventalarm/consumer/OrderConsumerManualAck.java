package com.example.kafkaeventalarm.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumerManualAck {

    private final Logger logger = LoggerFactory.getLogger(OrderConsumerManualAck.class);

    // @KafkaListener(topics = "#{'${order.topic.name}'.split(',')}", groupId = Constants.GROUP_ID, containerFactory = "orderKafkaListenerContainerFactoryManualAck")
    // public void consume(Order order) {
    //     logger.info(String.format("Order consumed (manual ack) -> %s", order));
    // }
}

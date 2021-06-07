package com.example.kafkaeventalarm.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.kafkaeventalarm.Constants;
import com.example.kafkaeventalarm.model.Order;

@Service
public class OrderConsumer {
    private final Logger logger = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(topics = "#{'${order.topic.name}'.split(',')}", groupId = Constants.GROUP_ID, containerFactory = "orderKafkaListenerContainerFactory")
    public void consume(Order order) {
       logger.info(String.format("Order consumed (auto ack)-> %s", order));
   }
}

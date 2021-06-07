package com.example.kafkaeventalarm.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.kafkaeventalarm.Constants;
import com.example.kafkaeventalarm.model.Return;

@Service
public class ReturnConsumer {

    private final Logger logger = LoggerFactory.getLogger(ReturnConsumer.class);

    @KafkaListener(topics = "#{'${return.topic.name}'.split(',')}", groupId = Constants.GROUP_ID, containerFactory = "returnKafkaListenerContainerFactory")
    public void consume(Return aReturn) {
        logger.info(String.format("Return consumed -> %s", aReturn));
    }
}

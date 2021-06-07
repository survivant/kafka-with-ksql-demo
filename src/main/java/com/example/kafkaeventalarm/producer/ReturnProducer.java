package com.example.kafkaeventalarm.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.kafkaeventalarm.model.Return;

@Service
public class ReturnProducer {

    private static final Logger logger = LoggerFactory.getLogger(ReturnProducer.class);

    @Value("${return.topic.name}")
    private String inputTopic;

    @Autowired
    private KafkaTemplate<String, Return> kafkaTemplate;

    public void sendMessage(Return aReturn) {
        logger.debug(String.format("#### -> Producing message -> %s", aReturn));
        kafkaTemplate.send(inputTopic, aReturn.getReturnId(), aReturn);
    }
}

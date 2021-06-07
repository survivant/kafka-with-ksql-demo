package com.example.kafkaeventalarm.controller;

import com.example.kafkaeventalarm.admin.KafkaAdminClient;
import io.confluent.ksql.api.client.QueryInfo;
import io.confluent.ksql.api.client.StreamInfo;
import io.confluent.ksql.api.client.TableInfo;
import io.confluent.ksql.api.client.TopicInfo;

import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/kafka")
public class KafkaAdminController {

    @Autowired
    private KafkaAdminClient kafkaAdminClient;

    @GetMapping(value = "/getTopics")
    public Collection<String> getTopics() throws Exception {
        var topicListings = kafkaAdminClient.getTopics().listings().get();

        return topicListings.stream().map(topicListing -> topicListing.name()).collect(Collectors.toList());
    }

    @GetMapping(value = "/getConsumerGroups")
    public Collection<String> getConsumerGroups() throws Exception {
        var consumerGroupListings = kafkaAdminClient.getConsumeGroups().all().get();

        return consumerGroupListings.stream().map(consumerGroupListing -> consumerGroupListing.groupId()).collect(Collectors.toList());
    }

    @GetMapping(value = "/describeTopic/{topic}")
    public Map<String, TopicDescription> describeTopic(@RequestParam String topic) throws Exception {
        var describeTopic = kafkaAdminClient.getDescribeTopic(topic);

        var map = describeTopic.all().get();
        // nothing useful for now
        return map;
    }

    @GetMapping(value = "/listTopics")
    public List<TopicInfo> listTopics() throws ExecutionException, InterruptedException {
        return kafkaAdminClient.listTopics().get();
    }

    @GetMapping(value = "/listStreams")
    public List<StreamInfo> listStreams() throws ExecutionException, InterruptedException {
        return kafkaAdminClient.listStreams().get();
    }

    @GetMapping(value = "/listQueries")
    public List<QueryInfo> listQueries() throws ExecutionException, InterruptedException {
        return kafkaAdminClient.listQueries().get();
    }

    @GetMapping(value = "/listTables")
    public List<TableInfo> listTables() throws ExecutionException, InterruptedException {
        return kafkaAdminClient.listTables().get();
    }

}
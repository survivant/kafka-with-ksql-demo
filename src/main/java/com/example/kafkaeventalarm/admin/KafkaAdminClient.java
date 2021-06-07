package com.example.kafkaeventalarm.admin;

import org.apache.kafka.clients.admin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.QueryInfo;
import io.confluent.ksql.api.client.StreamInfo;
import io.confluent.ksql.api.client.TableInfo;
import io.confluent.ksql.api.client.TopicInfo;

@Service
public class KafkaAdminClient {

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Autowired
    private Client client;


    public ListTopicsResult getTopics(){
        var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress));

        return admin.listTopics();
    }

    public DescribeTopicsResult getDescribeTopic(String topic){
        var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress));

        return admin.describeTopics(Arrays.asList(topic));
    }

    public ListConsumerGroupsResult getConsumeGroups(){
        var admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress));

        return admin.listConsumerGroups();
    }

    public CompletableFuture<List<TopicInfo>> listTopics(){
        return client.listTopics();
    }

    public CompletableFuture<List<StreamInfo>> listStreams(){
        return client.listStreams();
    }

    public CompletableFuture<List<QueryInfo>> listQueries(){
        return client.listQueries();
    }

    public CompletableFuture<List<TableInfo>> listTables(){
        return client.listTables();
    }
}

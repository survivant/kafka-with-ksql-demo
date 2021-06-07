package com.example.kafkaeventalarm.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;

/**
 * This class is responsible with the configuration of the ksqldb client.
 */
@Configuration
public class KsqlDbConfig {

    @Value("${ksqldb.host}")
    public String ksqlDbHost;

    @Value("${ksqldb.port}")
    public Integer ksqlDbPort;

    @Bean
    public Client ksqlDbClient() {
        var options = ClientOptions.create()
                .setHost(ksqlDbHost)
                .setPort(ksqlDbPort);
        var client = Client.create(options);
        return client;
    }
}

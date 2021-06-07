package com.example.kafkaeventalarm.ksql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ExecuteStatementResult;
import io.confluent.ksql.api.client.Row;

@Component
public class KSQLStreams {

    @Value("${order.topic.name}")
    private String inputTopic;

    @Value("${return.topic.name}")
    private String returnTopic;

    @Autowired
    private Client client;

    @Autowired
    private KsqlDbStreamingQuery ksqlDbStreamingQuery;

    @PostConstruct
    public void createKSQLStreams() {

        var createOrderStream = "CREATE STREAM ksqlOrders (orderId VARCHAR, product VARCHAR, orderTimestamp VARCHAR, status VARCHAR) WITH (kafka_topic='" + inputTopic + "', value_format='json', partitions=1);";
        var createReturnStream = "CREATE STREAM ksqlReturns (returnId VARCHAR, orderId VARCHAR, returnTimestamp VARCHAR) WITH (kafka_topic='" + returnTopic + "', value_format='json', partitions=1);";

        // create streams from topics
        client.executeStatement(createOrderStream);
        client.executeStatement(createReturnStream);

        // ceci va créer une table qui va recevoir les updates du stream , FAIRE UN COUNT et contenir ces valeurs là..
        var sql = "CREATE TABLE ORDERS_BY_STATUS AS "
                + "SELECT status, COUNT(*) as COUNT "
                + "FROM ksqlOrders GROUP BY status EMIT CHANGES;";
        Map<String, Object> properties = new HashMap();
        properties.put("auto.offset.reset", "earliest");

        ExecuteStatementResult result = null;
        try {
            result = client.executeStatement(sql, properties).get();
            System.out.println("Query ID: " + result.queryId().orElse("<null>"));
        } catch (Exception e) {
            //e.printStackTrace();
        }

        // ceci permet de regrouper le count par fenetes de 1 minute
        var sql2 = "CREATE TABLE ORDERS_BY_STATUS_WINDOW AS "
                + "SELECT status, COUNT(*) as COUNT, "
                + "WINDOWSTART AS window_start, WINDOWEND AS window_end "
                + "FROM ksqlOrders "
                + "WINDOW TUMBLING (SIZE 60 SECONDS) "
                + "GROUP BY status EMIT CHANGES;";
        try {
            result = client.executeStatement(sql2, properties).get();
            System.out.println("Query ID: " + result.queryId().orElse("<null>"));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        // exemple
        // select STATUS, COUNT, TIMESTAMPTOSTRING(WINDOWSTART,'yyyy-MM-dd HH:mm:ss') AS WINDOW_START, TIMESTAMPTOSTRING(WINDOWEND,'yyyy-MM-dd HH:mm:ss') AS WINDOW_END from ORDERS_BY_STATUS_WINDOW where status = 'yyy';
        /*
        {"STATUS":"yyy","COUNT":1,"WINDOW_START":"2021-06-03 18:24:00","WINDOW_END":"2021-06-03 18:25:00"}
        {"STATUS":"yyy","COUNT":1,"WINDOW_START":"2021-06-03 18:16:00","WINDOW_END":"2021-06-03 18:17:00"}
        {"STATUS":"yyy","COUNT":1,"WINDOW_START":"2021-06-03 18:02:00","WINDOW_END":"2021-06-03 18:03:00"}
        {"STATUS":"yyy","COUNT":1,"WINDOW_START":"2021-06-03 18:01:00","WINDOW_END":"2021-06-03 18:02:00"}
        {"STATUS":"yyy","COUNT":11,"WINDOW_START":"2021-06-03 17:59:00","WINDOW_END":"2021-06-03 18:00:00"}
        {"STATUS":"yyy","COUNT":1,"WINDOW_START":"2021-06-03 17:57:00","WINDOW_END":"2021-06-03 17:58:00"}
         */



        // ceci va créer une table qui va recevoir les updates du stream
        sql = "CREATE TABLE ORDERS_VIEW WITH (KAFKA_TOPIC='ORDERS_VIEW', PARTITIONS=1, REPLICAS=1) AS SELECT\n" +
                "  KSQLORDERS.ORDERID ORDERID,\n" +
                "  LATEST_BY_OFFSET(KSQLORDERS.PRODUCT) PRODUCT,\n" +
                "  LATEST_BY_OFFSET(KSQLORDERS.ORDERTIMESTAMP) ORDERTIMESTAMP,\n" +
                "  LATEST_BY_OFFSET(KSQLORDERS.STATUS) STATUS\n" +
                "FROM KSQLORDERS KSQLORDERS\n" +
                "GROUP BY KSQLORDERS.ORDERID\n" +
                "EMIT CHANGES;";

        try {
            result = client.executeStatement(sql, properties).get();
            System.out.println("Query ID: " + result.queryId().orElse("<null>"));
        } catch (Exception e) {
            //e.printStackTrace();
        }


        // exemple de Push query.. comme un Consumer de stream update.
        createPushQueries();

    }

    public List<Row> listItemsInLastWindow() throws ExecutionException, InterruptedException {
        Map<String, Object> properties = new HashMap();
        properties.put("auto.offset.reset", "earliest");
        properties.put("ksql.query.pull.table.scan.enabled", Boolean.TRUE);

        var sql = "select STATUS, COUNT, TIMESTAMPTOSTRING(WINDOWSTART,'yyyy-MM-dd HH:mm:ss') AS WINDOW_START, TIMESTAMPTOSTRING(WINDOWEND,'yyyy-MM-dd HH:mm:ss') AS WINDOW_END from ORDERS_BY_STATUS_WINDOW where WINDOWEND > UNIX_TIMESTAMP();";
        var batchedQueryResult = client.executeQuery(sql, properties);
        return batchedQueryResult.get();
    }

    public List<Row> getItemsLastValue() throws ExecutionException, InterruptedException {
        Map<String, Object> properties = new HashMap();
        properties.put("auto.offset.reset", "earliest");
        properties.put("ksql.query.pull.table.scan.enabled", Boolean.TRUE);

        var sql = "select * from ORDERS_BY_STATUS;";
        var batchedQueryResult = client.executeQuery(sql, properties);
        return batchedQueryResult.get();
    }

    private void createPushQueries(){
        // will return
        /*
        {STATUS=bbb, WINDOWSTART=1622829600000, WINDOWEND=1622829660000, COUNT=1, WINDOW_START=1622829600000, WINDOW_END=1622829660000}
        {STATUS=bbb, WINDOWSTART=1622829600000, WINDOWEND=1622829660000, COUNT=2, WINDOW_START=1622829600000, WINDOW_END=1622829660000}
        ... on essaye plus tard.. et dans la nouvelle fenetre le count retourne a 1
        {STATUS=bbb, WINDOWSTART=1622829780000, WINDOWEND=1622829840000, COUNT=1, WINDOW_START=1622829780000, WINDOW_END=1622829840000}
         */
        ksqlDbStreamingQuery.query("select * from ORDERS_BY_STATUS_WINDOW emit changes;", (row)->{
            var objectMap = row.asObject().getMap();

            System.out.println("MESSAGE RECEIVED = " + objectMap);

        });

        // will return
        /*
        {STATUS=bbb, COUNT=10}
        {STATUS=bbb, COUNT=11}
        {STATUS=bbb, COUNT=13}
         */
        ksqlDbStreamingQuery.query("select * from ORDERS_BY_STATUS emit changes;", (row)->{
            var objectMap = row.asObject().getMap();

            System.out.println("MESSAGE2 RECEIVED = " + objectMap);

        });

        // will return
        /*
        {STATUS=bbb, COUNT=10}
        {STATUS=bbb, COUNT=11}
        {STATUS=bbb, COUNT=13}
         */
        ksqlDbStreamingQuery.query("select * from ORDERS_VIEW emit changes;", (row)->{
            var objectMap = row.asObject().getMap();

            System.out.println("MESSAGE3 RECEIVED = " + objectMap);

        });
    }

    public List<Row> getOrdersValue() throws ExecutionException, InterruptedException {
        Map<String, Object> properties = new HashMap();
        properties.put("auto.offset.reset", "earliest");
        properties.put("ksql.query.pull.table.scan.enabled", Boolean.TRUE);

        var sql = "select * from ORDERS_VIEW;";
        var batchedQueryResult = client.executeQuery(sql, properties);
        return batchedQueryResult.get();
    }
}

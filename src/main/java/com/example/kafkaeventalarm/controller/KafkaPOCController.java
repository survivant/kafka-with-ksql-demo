package com.example.kafkaeventalarm.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.kafkaeventalarm.ksql.KSQLStreams;
import com.example.kafkaeventalarm.manager.OrderManager;
import com.example.kafkaeventalarm.model.Order;
import com.example.kafkaeventalarm.model.Return;
import com.example.kafkaeventalarm.producer.OrderProducer;
import com.example.kafkaeventalarm.producer.ReturnProducer;
import io.confluent.ksql.api.client.KsqlArray;
import io.confluent.ksql.api.client.Row;

@RestController
@RequestMapping(value = "/kafka")
public class KafkaPOCController {

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private ReturnProducer returnProducer;

    @Autowired
    private OrderManager orderManager;

    @Autowired
    private KSQLStreams ksqlStreams;

    @PostMapping(value = "/createOrder")
    public void sendMessageToKafkaTopic(@RequestBody Order order) {
        this.orderProducer.sendMessage(order);
    }

    // retourne les items de la derniere fenetre uniquement, ca va couvera pas tous les items
    // interactiveQuery
    @GetMapping(value="/listItemsInLastWindow")
    public Map<String,Integer> listItemsInLastWindow() throws ExecutionException, InterruptedException {
        var rows = ksqlStreams.listItemsInLastWindow();

        var map = new HashMap<String,Integer>();
        for (var row : rows) {
            map.put(row.values().getString(0), row.values().getInteger(1));
        }

        return map;
    }

    // retourne la derniere valeur de tous les items
    // interactiveQuery
    @GetMapping(value="/getItemsLastValue")
    public Map<String,Integer> getItemsLastValue() throws ExecutionException, InterruptedException {
        var rows = ksqlStreams.getItemsLastValue();

        var map = new HashMap<String,Integer>();
        for (var row : rows) {
            map.put(row.values().getString(0), row.values().getInteger(1));
        }

        return map;
    }

    // retourne la derniere valeur de tous les items
    // interactiveQuery
    @GetMapping(value="/getOrdersValue")
    public Collection<KsqlArray> getOrdersValue() throws ExecutionException, InterruptedException {
        var rows = ksqlStreams.getOrdersValue();

        var list = new ArrayList<KsqlArray>();
        for (var row : rows) {
            list.add(row.values());
        }

        return list;
    }

    //le cas où on update un message.. c'est la meme logique que de créer un nouveau Order, sauf que la cle primaire est fourni..mais dans ce POC.. aucune différence
    @PostMapping(value = "updateOrder")
    public void updateOrder(@RequestBody Order order){
        // option 1 : utilise le producer
        orderProducer.sendMessage(order);
    }

    @PostMapping(value = "/createReturn")
    public void sendMessageToKafkaTopic(@RequestBody Return areturn) {
        returnProducer.sendMessage(areturn);
    }

    @GetMapping(value = "/getOrders")
    public List<Order> getOrders() {
        return orderManager.getAllOrders();
    }

    @GetMapping(value = "/getOrders/timestamp")
    public List<Order> getOrders(@RequestParam String timestamp) {
        return orderManager.getAllOrders(timestamp);
    }

    @GetMapping(value = "/getReturns")
    public List<Return> getReturns() {
        return Collections.emptyList();
    }

}
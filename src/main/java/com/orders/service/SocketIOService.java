package com.orders.service;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.SocketIOClient;
import com.orders.config.SocketIOConfig;
import com.orders.dto.Order;
import com.orders.dto.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@CrossOrigin
public class SocketIOService {
    @Autowired
    SocketIOConfig configuration;

    @Autowired
    private SocketIOServer server;
    @Autowired
    private  OrderService orderService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        System.out.println("Starting socket server at " + server.getConfiguration().getHostname() + ":" + server.getConfiguration().getPort());
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(this::onDisconnect);

        server.addEventListener("getOrders", Request.class, (client, request, ackRequest) -> {
            List<Order> orders = orderService.getFilteredAndSortedOrders(request);
            Map<String, Object> response = new HashMap<>();
            response.put("rowData", orders);
            response.put("lastRow", orderService.getTotalCount());
            client.sendEvent("orderUpdate", response);
        });

        // Start the server
        server.start();

        // Schedule order updates
        scheduler.scheduleAtFixedRate(() -> {
            Order newOrder = orderService.generateOrder();
            server.getBroadcastOperations().sendEvent("newOrder", newOrder);
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void onConnect(SocketIOClient client) {
        System.out.println("Client connected: " + client.getSessionId());
    }

    private void onDisconnect(SocketIOClient client) {
        System.out.println("Client disconnected: " + client.getSessionId());
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        server.stop();
    }
}
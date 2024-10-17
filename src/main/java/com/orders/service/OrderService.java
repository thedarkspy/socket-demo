package com.orders.service;

import com.orders.dto.Order;
import com.orders.dto.Request;
import com.orders.dto.FilterModel;
import com.orders.dto.SortModel;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@CrossOrigin
public class OrderService {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @PostConstruct
    public void init() {
        // Generate initial orders
        for (int i = 0; i < 100; i++) {
            generateOrder();
        }
    }

    public Order generateOrder() {
        String orderId = UUID.randomUUID().toString();
        Order order = Order.builder()
                .id(orderId)
                .price(random.nextDouble() * 1000)
                .date(Instant.now().minusMillis(random.nextInt(10000000)).toString())
                .build();
        orders.put(orderId, order);
        return order;
    }

    public List<Order> getFilteredAndSortedOrders(Request request) {
        List<Order> filteredData = new ArrayList<>(orders.values());

        // Apply filters
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            filteredData = filteredData.stream()
                    .filter(order -> applyFilters(order, request.getFilterModel()))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        if (request.getSortModel() != null && !request.getSortModel().isEmpty()) {
            applySorting(filteredData, request.getSortModel());
        }

        // Apply pagination
        int start = Math.min(request.getStartRow(), filteredData.size());
        int end = Math.min(request.getEndRow(), filteredData.size());
        return filteredData.subList(start, end);
    }

    private boolean applyFilters(Order order, Map<String, FilterModel> filterModel) {
        return filterModel.entrySet().stream().allMatch(entry -> {
            String field = entry.getKey();
            FilterModel filter = entry.getValue();
            String value = getFieldValue(order, field);

            if ("contains".equals(filter.getType())) {
                return value.toLowerCase().contains(filter.getFilter().toLowerCase());
            } else if ("equals".equals(filter.getType())) {
                return value.equals(filter.getFilter());
            }
            return true;
        });
    }

    private String getFieldValue(Order order, String field) {
        return switch (field) {
            case "id" -> order.getId();
            case "price" -> String.valueOf(order.getPrice());
            case "date" -> order.getDate();
            default -> "";
        };
    }

    private void applySorting(List<Order> data, List<SortModel> sortModel) {
        data.sort((a, b) -> {
            for (SortModel sort : sortModel) {
                int comparison = compareValues(getFieldValue(a, sort.getColId()),
                        getFieldValue(b, sort.getColId()));
                if (comparison != 0) {
                    return "asc".equals(sort.getSort()) ? comparison : -comparison;
                }
            }
            return 0;
        });
    }

    private int compareValues(String value1, String value2) {
        try {
            double d1 = Double.parseDouble(value1);
            double d2 = Double.parseDouble(value2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            return value1.compareTo(value2);
        }
    }

    public int getTotalCount() {
        return orders.size();
    }
}
package com.nst.orderservice.service;

import com.nst.orderservice.dto.InventoryResponse;
import com.nst.orderservice.dto.OrderLineItemsDto;
import com.nst.orderservice.dto.OrderRequest;
import com.nst.orderservice.model.Order;
import com.nst.orderservice.model.OrderLineItems;
import com.nst.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final WebClient.Builder webClientBuilder;
    private final OrderRepository orderRepository;
    public void placeOrder(OrderRequest orderRequest){

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

       List<OrderLineItems> orderLineItems= orderRequest.getOrderLineItemsDtoList()
                .stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItems);

       List<String> skuCodes= order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode).toList();
        //call inventory services and place order if product in stock
       InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
        boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::isInStock);

        if(allProductsInStock){
           orderRepository.save(order);
       }else {
           throw new IllegalArgumentException("Product is not in Stock Please Try Again Later!");
       }



    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderlineItemsDto){

        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderlineItemsDto.getPrice());
        orderLineItems.setQuantity(orderlineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderlineItemsDto.getSkuCode());

        return orderLineItems;
    }
}

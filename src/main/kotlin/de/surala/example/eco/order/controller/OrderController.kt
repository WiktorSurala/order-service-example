package de.surala.example.eco.order.controller

import de.surala.example.eco.order.data.db.Order
import de.surala.example.eco.order.dto.OrderRequest
import de.surala.example.eco.order.dto.OrderResponse
import de.surala.example.eco.order.service.OrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(@RequestBody request: OrderRequest): ResponseEntity<OrderResponse> {
        val response = orderService.createOrder(request)
        return ResponseEntity.status(201).body(response)
    }

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: String): ResponseEntity<Order> {
        val order = orderService.getOrderById(id)
        return ResponseEntity.ok(order)
    }
}
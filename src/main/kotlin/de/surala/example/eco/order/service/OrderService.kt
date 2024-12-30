package de.surala.example.eco.order.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.surala.example.eco.order.data.catalog.ProductDetails
import de.surala.example.eco.order.data.catalog.ProductDetailsList
import de.surala.example.eco.order.data.db.Order
import de.surala.example.eco.order.data.db.OrderProduct
import de.surala.example.eco.order.dto.OrderRequest
import de.surala.example.eco.order.dto.OrderResponse
import de.surala.example.eco.order.dto.ProductIdRequest
import de.surala.example.eco.order.dto.ProductProcessingResult
import de.surala.example.eco.order.dto.ProductRequest
import de.surala.example.eco.order.repository.OrderRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    @Qualifier("catalogServiceRestTemplate") private val catalogServiceRestTemplate: RestTemplate,
    private val rabbitTemplate: RabbitTemplate
) {
    fun createOrder(request: OrderRequest): OrderResponse {
        // Fetch product details from Catalog Service
        val products = processOrder(request)

        // Calculate total price
        val totalPrice = products.sumOf { (orderProduct, price) -> price * orderProduct.quantity }

        // Save order
        val order = Order(
            userId = request.userId,
            products = products.map { it.first },
            totalPrice = totalPrice
        )

        val savedOrder = orderRepository.save(order)

        // Publish to RabbitMQ
        rabbitTemplate.convertAndSend("order.exchange", "order.created", savedOrder)

        return OrderResponse(savedOrder.id!!, savedOrder.status)
    }

    // Refactored Order Processing Function
    fun processOrder(request: OrderRequest): List<Pair<OrderProduct, Double>> {

        val productIds = request.products.map { it.productId }.distinct()

        val productsDetails = fetchProductDetails(productIds).products

        val productDetailsMap = productsDetails.associateBy { it.id }

        val products = mutableListOf<Pair<OrderProduct, Double>>()
        val productRequestErrors = mutableListOf<String>()

        for (productRequest in request.products) {
            when (val result = processProductRequest(productDetailsMap, productRequest)) {
                is ProductProcessingResult.Success -> {
                    products.add(result.orderProduct to result.price)
                }

                is ProductProcessingResult.Error -> {
                    productRequestErrors.add(result.message)
                }
            }
        }

        if (productRequestErrors.isNotEmpty()) {
            val errorMessage = productRequestErrors.joinToString("; ")
            throw IllegalArgumentException(errorMessage)
        }

        return products
    }

    private fun processProductRequest(
        productDetailsMap: Map<String, ProductDetails>,
        productRequest: ProductRequest
    ): ProductProcessingResult {
        val productDetails = productDetailsMap[productRequest.productId]
            ?: return ProductProcessingResult.Error("Product ${productRequest.productId} not found.")

        if (productDetails.stock < productRequest.quantity) {
            return ProductProcessingResult.Error(
                "Insufficient stock for product ${productRequest.productId}. " +
                        "Available: ${productDetails.stock}, Requested: ${productRequest.quantity}."
            )
        }

        // If stock is sufficient, return success
        return ProductProcessingResult.Success(
            OrderProduct(
                productId = productRequest.productId,
                quantity = productRequest.quantity
            ), productDetails.price
        )
    }


    fun getOrderById(id: String): Order? {
        return orderRepository.findById(id).orElse(null)
    }

    fun fetchProductDetails(productIds: List<String>): ProductDetailsList {
        // Construct the request body
        val requestBody = ProductIdRequest(productIds)

        // Set headers
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val objectMapper = ObjectMapper()
        val jsonBody: String = objectMapper.writeValueAsString(requestBody)

        // Create HttpEntity with headers and body
        val requestEntity = HttpEntity(jsonBody, headers)

        // Make POST request
        val response: ResponseEntity<ProductDetailsList> = catalogServiceRestTemplate.postForEntity(
            "/products/details",
            requestEntity,
            ProductDetailsList::class.java
        )


        // Return response body or throw an exception
        return response.body ?: throw RuntimeException("Failed to fetch product details")
    }

    fun updateOrderStatus(orderId: String, updatedStatus: String): OrderResponse {
        val existingOrder = orderRepository.findById(orderId)
            .orElseThrow { OrderNotFoundException("Order with id $orderId not found") }

        existingOrder.status = updatedStatus
        val savedOrder = orderRepository.save(existingOrder)
        rabbitTemplate.convertAndSend("order.exchange", "order.updated", savedOrder)
        return OrderResponse(savedOrder.id.toString(), savedOrder.status)
    }

}

class OrderNotFoundException(message: String) : RuntimeException(message)
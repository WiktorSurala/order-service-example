package de.surala.example.eco.order.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.surala.example.eco.order.data.catalog.ErrorResponse
import de.surala.example.eco.order.data.catalog.ProductDetails
import de.surala.example.eco.order.data.catalog.ProductDetailsList
import de.surala.example.eco.order.data.db.Order
import de.surala.example.eco.order.data.db.OrderProduct
import de.surala.example.eco.order.dto.OrderRequest
import de.surala.example.eco.order.dto.OrderResponse
import de.surala.example.eco.order.dto.ProductIdRequest
import de.surala.example.eco.order.dto.ProductProcessingResult
import de.surala.example.eco.order.dto.ProductRequest
import de.surala.example.eco.order.exception.InvalidProductIdException
import de.surala.example.eco.order.exception.OrderNotFoundException
import de.surala.example.eco.order.repository.OrderRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    @Qualifier("catalogServiceRestTemplate") private val catalogServiceRestTemplate: RestTemplate,
    private val rabbitTemplate: RabbitTemplate
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

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
        logger.info("Fetching product details for productIds: {}", productIds)

        // Construct the request body
        val requestBody = ProductIdRequest(productIds)

        // Set headers
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val objectMapper = ObjectMapper()
        val jsonBody: String = objectMapper.writeValueAsString(requestBody)

        // Log request details
        logger.debug("Request body: {}", jsonBody)
        logger.debug("Request headers: {}", headers)

        // Create HttpEntity with headers and body
        val requestEntity = HttpEntity(jsonBody, headers)

        try {
            // Log before sending the request
            logger.info("Sending POST request to /products/details")

            // Make POST request
            val response: ResponseEntity<ProductDetailsList> = catalogServiceRestTemplate.postForEntity(
                "/products/details",
                requestEntity,
                ProductDetailsList::class.java
            )

            // Log response details
            logger.info("Received response with status code: {}", response.statusCode)
            logger.debug("Response body: {}", response.body)

            // Return response body or throw an exception
            return response.body ?: throw RuntimeException("Failed to fetch product details")
        } catch (ex: HttpClientErrorException) {
            // Handle 400 Bad Request specifically
            if (ex.statusCode == HttpStatus.BAD_REQUEST) {
                logger.error("Bad Request: {}", ex.responseBodyAsString)

                // Parse the error response (if needed)
                val errorResponse = objectMapper.readValue(ex.responseBodyAsString, ErrorResponse::class.java)
                throw InvalidProductIdException("Invalid product IDs: ${errorResponse.message}")
            } else {
                logger.error("Client error occurred: {}", ex.message)
                throw ex
            }
        } catch (ex: Exception) {
            // Log and rethrow other exceptions
            logger.error("An error occurred while fetching product details: {}", ex.message)
            throw ex
        }
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

import de.surala.example.eco.order.data.catalog.ProductDetails
import de.surala.example.eco.order.data.catalog.ProductDetailsList
import de.surala.example.eco.order.data.db.Order
import de.surala.example.eco.order.data.db.OrderProduct
import de.surala.example.eco.order.dto.OrderRequest
import de.surala.example.eco.order.dto.OrderResponse
import de.surala.example.eco.order.dto.ProductIdRequest
import de.surala.example.eco.order.dto.ProductRequest
import de.surala.example.eco.order.exception.OrderNotFoundException
import de.surala.example.eco.order.repository.OrderRepository
import de.surala.example.eco.order.service.OrderService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

@ExtendWith(SpringExtension::class)
class OrderServiceTest {

    // Mock dependencies
    private lateinit var orderRepository: OrderRepository
    private lateinit var catalogServiceRestTemplate: RestTemplate
    private lateinit var rabbitTemplate: RabbitTemplate

    // The service under test
    private lateinit var orderService: OrderService

    // Shared example order
    private val defaultOrder: Order = Order(
        id = UUID.randomUUID().toString(),
        userId = "user123",
        products = listOf(OrderProduct("prod1", 2)),
        totalPrice = 59.98,
        status = "Pending",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        // Initialize mocks
        orderRepository = mockk()
        catalogServiceRestTemplate = mockk()
        rabbitTemplate = mockk()

        // Initialize service with mocked dependencies
        orderService = OrderService(orderRepository, catalogServiceRestTemplate, rabbitTemplate)
    }

    @Test
    fun `getOrderById should return order when order exists`() {
        // Arrange
        val orderId = UUID.randomUUID().toString()

        every { orderRepository.findById(orderId) } returns Optional.of(defaultOrder)

        // Act
        val foundOrder = orderService.getOrderById(orderId)

        // Assert
        verify(exactly = 1) { orderRepository.findById(orderId) }
        assertNotNull(foundOrder, "Order should not be null")
        assertEquals("user123", foundOrder?.userId)
        assertEquals(59.98, foundOrder?.totalPrice)
    }

    @Test
    fun `getOrderById should return null when order does not exist`() {
        // Arrange
        val orderId = UUID.randomUUID().toString()
        every { orderRepository.findById(orderId) } returns Optional.empty()

        // Act
        val foundOrder = orderService.getOrderById(orderId)

        // Assert
        verify(exactly = 1) { orderRepository.findById(orderId) }
        assertNull(foundOrder, "Order should be null when not found")
    }

    @Test
    fun `createOrder should save order and send message`() {
        // Arrange
        val orderRequest = defaultOrder.toOrderRequest()

        val expectedOrderResponse = OrderResponse(defaultOrder.id.toString(), defaultOrder.status)

        every { orderRepository.save(any<Order>()) } returns defaultOrder
        every { rabbitTemplate.convertAndSend("order.exchange", "order.created", any<Order>()) } just Runs

        val mockedProductDetails = ProductDetailsList(
            listOf(
                ProductDetails(id = "prod1", name = "Product 1", price = 10.0, stock = 100),
                ProductDetails(id = "prod2", name = "Product 2", price = 20.0, stock = 200)
            )
        )

        val mockedResponseEntity = ResponseEntity.ok(mockedProductDetails)

        every {
            catalogServiceRestTemplate.postForEntity(
                "/products/details",
                any<HttpEntity<ProductIdRequest>>(),
                ProductDetailsList::class.java
            )
        } returns mockedResponseEntity


        // Act
        val result = orderService.createOrder(orderRequest)


        // Assert
        verify(exactly = 1) { orderRepository.save(any<Order>()) }
        verify(exactly = 1) { rabbitTemplate.convertAndSend("order.exchange", "order.created", any<Order>()) }
        assertEquals(expectedOrderResponse, result, "The saved order should be returned")
    }

    @Test
    fun `updateOrderStatus should update status and send message when order exists`() {
        // Arrange
        val orderId = UUID.randomUUID().toString()
        val existingOrder = Order(
            id = orderId,
            userId = "user123",
            products = listOf(OrderProduct("prod1", 2)),
            totalPrice = 59.98,
            status = "Pending",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val updatedStatus = "Shipped"
        val updatedOrder = existingOrder.copy(
            status = updatedStatus,
            updatedAt = LocalDateTime.now()
        )

        every { orderRepository.findById(orderId) } returns Optional.of(existingOrder)
        every { orderRepository.save(any<Order>()) } returns updatedOrder
        every { rabbitTemplate.convertAndSend("order.exchange", "order.updated", updatedOrder) } just Runs

        // Act
        val result = orderService.updateOrderStatus(orderId, updatedStatus)

        // Assert
        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 1) { orderRepository.save(match { it.id == orderId && it.status == updatedStatus }) }
        verify(exactly = 1) { rabbitTemplate.convertAndSend("order.exchange", "order.updated", updatedOrder) }
        assertNotNull(result, "Updated order should not be null")
        assertEquals(updatedStatus, result.status, "Order status should be updated")
    }

    @Test
    fun `updateOrderStatus should throw OrderNotFoundException when order does not exist`() {
        // Arrange
        val orderId = UUID.randomUUID().toString()
        val updatedStatus = "Shipped"

        every { orderRepository.findById(orderId) } returns Optional.empty()

        // Act & Assert
        val exception = assertThrows<OrderNotFoundException> {
            orderService.updateOrderStatus(orderId, updatedStatus)
        }

        // Assert Exception Message
        assertEquals("Order with id $orderId not found", exception.message)

        // Verify interactions
        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 0) { orderRepository.save(any<Order>()) }
        verify(exactly = 0) { rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<Order>()) }
    }

    // Helper function to create OrderRequest from Order
    private fun Order.toOrderRequest(): OrderRequest {
        return OrderRequest(
            userId = this.userId,
            products = this.products.map { it.toProductRequest() }
        )
    }

    // Extension function to convert OrderProduct to ProductRequest
    private fun OrderProduct.toProductRequest(): ProductRequest {
        return ProductRequest(
            productId = this.productId,
            quantity = this.quantity
        )
    }

}

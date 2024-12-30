package de.surala.example.eco.order.data.db

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "orders")
data class Order(
    @Id val id: String? = null,
    val userId: String,
    val products: List<OrderProduct>,
    val totalPrice: Double,
    var status: String = "Pending",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

data class OrderProduct(
    val productId: String,
    val quantity: Int
)

package de.surala.example.eco.order.repository

import de.surala.example.eco.order.data.db.Order
import org.springframework.data.mongodb.repository.MongoRepository

interface OrderRepository : MongoRepository<Order, String>
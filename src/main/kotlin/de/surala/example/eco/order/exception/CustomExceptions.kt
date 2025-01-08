package de.surala.example.eco.order.exception

class OrderNotFoundException(message: String) : RuntimeException(message)
class InvalidProductIdException(message: String) : RuntimeException(message)

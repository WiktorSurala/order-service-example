package de.surala.example.eco.order.dto

import de.surala.example.eco.order.data.db.OrderProduct

data class OrderRequest(
        val userId:String,
        val products:List<ProductRequest>
)

data class ProductRequest(
        val productId:String,
        val quantity:Int
)

data class OrderResponse(
        val orderId:String,
        val status:String
)

data class ProductIdRequest(
        val productIds: List<String>
)

sealed class ProductProcessingResult {
        data class Success(val orderProduct: OrderProduct, val price: Double) : ProductProcessingResult()
        data class Error(val message: String) : ProductProcessingResult()
}

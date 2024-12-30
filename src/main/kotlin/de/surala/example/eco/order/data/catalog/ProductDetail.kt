package de.surala.example.eco.order.data.catalog

data class ProductDetails(
    val id: String,
    val name: String,
    val price: Double,
    val stock: Int
)

data class ProductDetailsList(val products: List<ProductDetails>)
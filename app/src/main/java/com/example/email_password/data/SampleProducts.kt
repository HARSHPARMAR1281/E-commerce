package com.example.email_password.data

import com.example.email_password.model.Product

object SampleProducts {
    val products = listOf(
        // Electronics
        Product(
            id = "e1",
            name = "iPhone 14 Pro",
            description = "Latest iPhone with A16 Bionic chip, 48MP camera, and Dynamic Island",
            price = 999.99,
            imageUrl = "https://images.unsplash.com/photo-1678652197831-2d18004f1259",
            category = "Electronics",
            stock = 50,
            rating = 4.8f
        ),
        Product(
            id = "e2",
            name = "Samsung Galaxy S23",
            description = "Powerful Android smartphone with Snapdragon 8 Gen 2",
            price = 899.99,
            imageUrl = "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c",
            category = "Electronics",
            stock = 45,
            rating = 4.7f
        ),
        Product(
            id = "e3",
            name = "MacBook Pro M2",
            description = "Powerful laptop with Apple M2 chip and Retina display",
            price = 1299.99,
            imageUrl = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8",
            category = "Electronics",
            stock = 30,
            rating = 4.9f
        ),

        // Clothing
        Product(
            id = "c1",
            name = "Men's Casual Jacket",
            description = "Stylish and comfortable jacket for everyday wear",
            price = 79.99,
            imageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea",
            category = "Clothing",
            stock = 100,
            rating = 4.5f
        ),
        Product(
            id = "c2",
            name = "Women's Summer Dress",
            description = "Light and elegant summer dress with floral pattern",
            price = 59.99,
            imageUrl = "https://images.unsplash.com/photo-1572804013309-59a88b7e92f1",
            category = "Clothing",
            stock = 75,
            rating = 4.6f
        ),

        // Books
        Product(
            id = "b1",
            name = "The Great Gatsby",
            description = "Classic novel by F. Scott Fitzgerald",
            price = 14.99,
            imageUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f",
            category = "Books",
            stock = 200,
            rating = 4.8f
        ),
        Product(
            id = "b2",
            name = "To Kill a Mockingbird",
            description = "Harper Lee's masterpiece about justice and racial inequality",
            price = 12.99,
            imageUrl = "https://images.unsplash.com/photo-1541963463532-d68292c34b19",
            category = "Books",
            stock = 150,
            rating = 4.9f
        ),

        // Home & Kitchen
        Product(
            id = "h1",
            name = "Smart Coffee Maker",
            description = "WiFi-enabled coffee maker with app control",
            price = 149.99,
            imageUrl = "https://images.unsplash.com/photo-1572442388796-11668a67e53d",
            category = "Home & Kitchen",
            stock = 40,
            rating = 4.7f
        ),
        Product(
            id = "h2",
            name = "Stainless Steel Cookware Set",
            description = "10-piece cookware set with non-stick coating",
            price = 199.99,
            imageUrl = "https://images.unsplash.com/photo-1584990347449-a2d4c2f1fce1",
            category = "Home & Kitchen",
            stock = 25,
            rating = 4.6f
        ),

        // Beauty
        Product(
            id = "be1",
            name = "Luxury Skincare Set",
            description = "Complete skincare routine with cleanser, toner, and moisturizer",
            price = 89.99,
            imageUrl = "https://images.unsplash.com/photo-1571781926291-c477ebfd024b",
            category = "Beauty",
            stock = 60,
            rating = 4.8f
        ),
        Product(
            id = "be2",
            name = "Professional Makeup Kit",
            description = "Complete makeup collection with brushes and case",
            price = 129.99,
            imageUrl = "https://images.unsplash.com/photo-1596462502278-27bfdc403348",
            category = "Beauty",
            stock = 35,
            rating = 4.7f
        ),

        // Sports
        Product(
            id = "s1",
            name = "Professional Yoga Mat",
            description = "Non-slip yoga mat with carrying strap",
            price = 39.99,
            imageUrl = "https://images.unsplash.com/photo-1592432678016-e910b452f9a2",
            category = "Sports",
            stock = 80,
            rating = 4.6f
        ),
        Product(
            id = "s2",
            name = "Running Shoes",
            description = "Lightweight running shoes with cushioned sole",
            price = 89.99,
            imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
            category = "Sports",
            stock = 55,
            rating = 4.8f
        ),

        // Toys
        Product(
            id = "t1",
            name = "Educational Robot Kit",
            description = "Build and program your own robot",
            price = 79.99,
            imageUrl = "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158",
            category = "Toys",
            stock = 45,
            rating = 4.7f
        ),
        Product(
            id = "t2",
            name = "Plush Teddy Bear",
            description = "Soft and cuddly teddy bear for all ages",
            price = 24.99,
            imageUrl = "https://images.unsplash.com/photo-1562040506-a9b32cb51b94",
            category = "Toys",
            stock = 100,
            rating = 4.9f
        )
    )
} 
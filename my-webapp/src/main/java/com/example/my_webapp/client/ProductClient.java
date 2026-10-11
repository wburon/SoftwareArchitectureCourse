package com.example.my_webapp.client;

import com.example.my_webapp.model.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "productClient", url = "http://localhost:8080")
public interface ProductClient {
    @GetMapping("/api/products")
    List<Product> getAllProducts();

    @PostMapping("/api/products")
    Product addProduct(Product product);
}
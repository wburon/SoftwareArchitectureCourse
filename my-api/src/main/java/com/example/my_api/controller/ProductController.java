package com.example.my_api.controller;

import com.example.my_api.model.Product;
import com.example.my_api.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<Product> addOneProduct(@RequestBody Product product) {
        var createdProduct = productService.addProduct(product);
        URI location = URI.create("/api/products/"+createdProduct.getId());
        return ResponseEntity.created(location).body(createdProduct);
    }
}
package com.example.my_webapp.controller;

import com.example.my_webapp.client.ProductClient;
import com.example.my_webapp.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductClient productClient;

    @GetMapping
    public String getProducts(Model model) {
        List<Product> products = productClient.getAllProducts();
        model.addAttribute("products", products);
        return "products";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        return "product-form";
    }

    @PostMapping
    public String createProduct(@ModelAttribute Product product) {
        productClient.addProduct(product);
        return "redirect:/products";
    }
}
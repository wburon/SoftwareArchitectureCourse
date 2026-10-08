# TP : Créer une Application Web Spring Boot Consommant une API avec Thymeleaf, Bootstrap, et Feign

## Objectif :
L'objectif est de créer une application web Spring Boot qui consomme une API REST et affiche des données sur des pages web. Nous utiliserons :
- **Thymeleaf** : pour générer des pages HTML dynamiques.
- **Bootstrap** : pour styliser l'interface et la rendre responsive.
- **Feign** : pour interagir facilement avec l'API qui gère des produits.

---

## Étape 1 : Initialisation du projet Spring Boot

1. Rendez-vous sur [Spring Initializr](https://start.spring.io/). **Sur votre VM...**
2. Configurez le projet avec les paramètres suivants :
   - **Project** : Maven Project
   - **Language** : Java
   - **Spring Boot** : Version 4.1.1
   - **Group** : `com.example`
   - **Artifact** : `mywebapp`
   - **Packaging** : Jar
   - **Java** : 21

3. Ajoutez les **dépendances** suivantes :
   - **Spring Web** : Pour construire l'application web (Contrôleurs REST).
   - **Thymeleaf** : Pour générer les pages HTML dynamiques.
   - **Spring Cloud OpenFeign** : Pour consommer l'API externe.
   
4. Cliquez sur **Generate** et extrayez le projet téléchargé dans votre environnement de développement (ex. IntelliJ IDEA, Eclipse).

---

## Étape 2 : Déclarer le client Feign pour consommer l'API

Créez une interface FeignClient qui correspond aux appels de votre API MyAPI.

1. Créez un nouveau package **com.example.mywebapp.client**.
2. Dans ce package, créez une interface **ProductClient.java** :
```java
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "productClient", url = "http://localhost:8080")
public interface ProductClient {
    @GetMapping("/api/products")
    List<Product> getAllProducts();
}
```

## Étape 3 : Créer un modèle Product 

Vous aurez besoin d'une classe Product pour représenter les données reçues de l'API.

- Question : êtes-vous capable de générer les classes (Object) à partir du swagger de l'API ? 

**Si oui.. a vous de jouer !** 
(en autonomie)
Indice : openapi-generator-maven-plugin

**Si non.. on continue !**
1. Créez un package **com.example.mywebapp.model**.
2. Créez une classe **Product.java** :
```java
public class Product {
    private Long id;
    private String name;
    private double price;

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
```

## Étape 4 : Créer un contrôleur Spring MVC

Ce contrôleur va appeler l'API via Feign et passer les données à Thymeleaf pour l'affichage.

1. Créez un package **com.example.mywebapp.controller**.
2. Créez une classe **ProductController.java** :
```java
import com.example.mywebapp.client.ProductClient;
import com.example.mywebapp.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class ProductController {

    @Autowired
    private ProductClient productClient;

    @GetMapping("/products")
    public String getProducts(Model model) {
        List<Product> products = productClient.getAllProducts();
        model.addAttribute("products", products);
        return "products";
    }
}
```

## Étape 5 : Créer la vue Thymeleaf

Nous allons maintenant créer une page HTML pour afficher la liste des produits récupérés via Feign.

1. Créez un dossier **src/main/resources/templates** (si ce n'est pas déjà fait).
2. Ajoutez un fichier **products.html** dans ce dossier :
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Liste des produits</title>
    <!-- Import Bootstrap -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css">
</head>
<body>
<div class="container">
    <h1 class="mt-5">Liste des produits</h1>
    <table class="table table-striped mt-3">
        <thead>
            <tr>
                <th>ID</th>
                <th>Nom</th>
                <th>Prix</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="product : ${products}">
                <td th:text="${product.id}">1</td>
                <td th:text="${product.name}">Produit</td>
                <td th:text="${product.price}">10.00</td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

## Étape 6 : Lancer l'application

1. Démarrez votre API (si elle n'est pas déjà en cours d'exécution).
2. Ensuite, lancez votre application web Spring Boot : mywebapp.
3. Si tout ce passe bien aller à l'adresse suivante dans un navigateur : **http://localhost:8080/products**

**Tout n'a pas du bien se passer ! A vous de jouer pour corriger !**

Indice : Il y a deux problèmes ! 
- L'un est lié a une annotation
- L'autre a un conflit dans la configuration de votre api et de votre application web

## Étape 7 : Amélioration de votre application web

Objectif : Proposer sur la webapp un formulaire pour ajouter un produit dans la base de données. 

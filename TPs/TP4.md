# TP : Générer la documentation Swagger d'une API Spring Boot

Swagger est un outil puissant pour générer la documentation des API REST. Avec Springdoc OpenAPI, il est facile d'intégrer Swagger dans un projet **Spring Boot** et d'obtenir une interface interactive pour tester et explorer les endpoints de votre API.

---

### Étape 1 : Ajouter la dépendance Springdoc OpenAPI

Dans votre fichier `pom.xml`, ajoutez la dépendance suivante pour intégrer Swagger avec Spring Boot :

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>?</version>
</dependency>
```
Cette dépendance intègre automatiquement Swagger et OpenAPI pour votre API.

- Question : Où trouver le numéro de la dernière version stable de cette dépendance ?

Indice : Il s'agit d'un registre.

### Étape 2 : Configurer Swagger

Aucune configuration supplémentaire n'est nécessaire. Springdoc configure automatiquement Swagger une fois la dépendance ajoutée.
Si vous souhaitez personnaliser l'URL d'accès à la documentation, vous pouvez configurer les propriétés suivantes dans **application.properties**

```properties
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```
- **/v3/api-docs** : L'URL pour accéder à la documentation OpenAPI en format JSON.
- **/swagger-ui.html** : L'URL pour accéder à l'interface utilisateur Swagger UI.

### Étape 3 : Démarrer l'application et accéder à Swagger

1. Démarrez votre application Spring Boot
2. Accédez à l'interface Swagger UI via le navigateur à l'URL suivante :
```bash
localhost:8080/swagger-ui.html
```

### Étape 4 : Ajouter des descriptions à vos endpoints avec des annotations Swagger

Vous pouvez améliorer la documentation en ajoutant des annotations à vos contrôleurs et méthodes. Par exemple, pour le contrôleur ProductController :
```java
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product API", description = "API for managing products")
public class ProductController {

    @Operation(summary = "Get all products", description = "Retrieve all products from the database")
    @GetMapping
    public List<Product> getAllProducts() {
        // Récupère tous les produits
    }

    // Autres méthodes...
}
```
**Annotations courantes utilisées avec Swagger**
- **@Operation** : Fournit une description pour chaque endpoint de l'API.
- **@Tag** : Définit des groupes et des descriptions pour les différentes parties de l'API.
- **@Parameter** : Personnalise la documentation des paramètres d'entrée des endpoints.

### Étape 5 : Personnaliser l'interface Swagger

Vous pouvez personnaliser les informations de votre API en créant une configuration OpenAPI. Créez une classe **SwaggerConfig** dans un nouveau package **config** dans **com.example.myapi** :
```java
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Product API")
                .version("1.0")
                .description("API for managing products with Spring Boot")
            );
    }
}
```

### Étape 6 : Tester Swagger

1. Une fois l'application démarrée, accéder à Swagger UI
2. Vous verrez la documentation complète de vos endpoints, et vous pourrez les tester directement via l'interface web.

### Étape 7 : Un premier test unitaire avec Junit

#### 7.1 : Ajouter les dépendances pour les tests
Si ce n'est pas déjà fait, assurez-vous que votre fichier pom.xml inclut les dépendances pour JUnit 5, Mockito, et Spring Boot Test.
```xml
<dependencies>
    <!-- Dépendance pour Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>?</version> <!-- Utilisez la dernière version -->
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 7.2 : Créer le test JUnit pour getAllProducts()

Voici un exemple de test JUnit pour la méthode **getAllProducts()** du contrôleur **ProductController**. Nous allons utiliser Mockito pour simuler le service **ProductService**.

**Structure du test**
- **Mocker** le service ProductService.
- Utiliser **MockMvc** pour tester les endpoints de l'API REST.
- Vérifier la réponse HTTP, le corps de la réponse, et le statut.

1. Crée un fichier de test ProductControllerTest.java dans le répertoire **src/test/java/com/example/api/controller/**
2. Ajoutez le contenu suivant :
```java
package com.example.api.controller;

import com.example.api.model.Product;
import com.example.api.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllProducts() throws Exception {
        // Données fictives
        List<Product> mockProducts = Arrays.asList(
                new Product(1L, "Product 1", 100.0),
                new Product(2L, "Product 2", 150.0)
        );

        // Simulation du comportement du service
        when(productService.getAllProducts()).thenReturn(mockProducts);

        // Appel de la méthode du contrôleur et vérification
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Vérifie que le statut HTTP est 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // Vérifie que le type de contenu est JSON
                .andExpect(jsonPath("$[0].name").value("Product 1")) // Vérifie que le premier produit a le nom "Product 1"
                .andExpect(jsonPath("$[1].name").value("Product 2")); // Vérifie que le deuxième produit a le nom "Product 2"
    }
}
```

@WebMvcTest(ProductController.class) : Indique que nous testons uniquement le contrôleur REST ProductController, sans démarrer toute l'application.
@MockBean : Utilisé pour injecter un mock du service ProductService dans le contexte Spring.
@Autowired : Injecte l'objet MockMvc pour simuler des requêtes HTTP.

#### 7.3 : Lancer  le test

Une fois le test lancé, vous devriez voir quelque chose comme ceci dans la sortie console de Maven.
```bash
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.785 s - in com.example.api.controller.ProductControllerTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```
Cela signifie que le test a bien été exécuté et que la méthode getAllProducts() retourne bien les données JSON attendues avec le bon statut HTTP.

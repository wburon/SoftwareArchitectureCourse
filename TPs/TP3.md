# TP : Créer une API Java Spring Boot avec une base de données PostgreSQL

## Objectifs

- Créer une API REST en Java avec Spring Boot.
- Utiliser PostgreSQL comme base de données.
- Effectuer les opérations CRUD (Create, Read, Update, Delete) sur une entité.

---

## Prérequis

- Avoir **Java 21** ou une version plus récente installée.
- Avoir **Maven** installé.
- Installer **PostgreSQL** et le configurer (créer une base de données).
- Avoir un IDE tel que **IntelliJ IDEA**, **Eclipse**, ou **VSCode** avec support Java.

---

## Étape 0 : Vérifier les prérequis

Nous allons commencer par vérifier les prérequis pour ce TP.

Vérifions tout d'abord la version de java actuellement installé sur la VM.
```sh
dev $ java -version
```
Normalement si tout se passe bien vous devriez voir apparaitre une sortie de la forme suivante mais avec Java21:
```text
openjdk version "11.0.24" 2024-07-16
OpenJDK Runtime Environment (build 11.0.24+8-post-Ubuntu-1ubuntu320.04)
OpenJDK 64-Bit Server VM (build 11.0.24+8-post-Ubuntu-1ubuntu320.04, mixed mode, sharing)
```

Vérifions maintenant que Maven est correctement installé et qu'il utilise la version Java 21 que nous venons d'installer.
```sh
dev $ mvn -version
```
Normalement si tout se passe bien vous devriez voir apparaitre une sortie de la forme suivante:
```sh
Apache Maven 3.9.4 (dfbb324ad4a7c8fb0bf182e6d91b0ae20e3d2dd9)
Maven home: /opt/maven
Java version: 17.0.12, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
Default locale: en, platform encoding: UTF-8
OS name: "linux", version: "5.15.0-1073-azure", arch: "amd64", family: "unix"
```
- Question : Est-ce Maven utilise bien la version Java 21 ?

Vérifions maintenant que Postgres est installé et qu'une base de données existes déjà.
```sh
dev $ psql --version
```
Normalement si tout se passe bien vous devriez voir apparaitre une sortie de la forme suivante:
```sh
psql (PostgreSQL) 12.20 (Ubuntu 12.20-0ubuntu0.20.04.1)
```
Dans le cas contraire, psql doit être installé avec la commande suivante
```sh
sudo apt update && sudo apt install postgresql postgresql-contrib
```
Testons maintenant la connexion à Postgres.
```sh
sudo -u postgres psql
```
Cela vous connectera à l'interface interactive de PostgreSQL si tout est configuré correctement. Une fois connecté, vous verrez un prompt psql comme ceci :
```
postgres=# 
```
Avec la commande "\l" vous listez toutes les bases de données pour vérifier que PostgreSQL fonctionne correctement.
Il est nécessaire pour la suite du TP de voir au moins apparaitre la base de données nommé "postgres".

Modifier le mot de passe d'accès à la base de donnée avec la commande 
```
ALTER USER postgres WITH PASSWORD 'password';
```
Pour quitter le prompt postgres tapez simplement "exit".

Vérifions maintenant qu'un IDE est disponible sur la machine virtuelle.
Par exemple IntelliJ IDEA.

Nous avons maintenant bien tout les prérequis pour attaquer sereinement ce TP.

## Étape 1 : Créer un projet Spring Boot

1. Rendez-vous sur [Spring Initializr](https://start.spring.io/).
2. Configurez le projet comme suit :
   - **Project**: Maven Project
   - **Language**: Java
   - **Spring Boot**: 4.1.1
   - **Group**: `com.example`
   - **Artifact**: `my-api`
   - **Package Name**: `com.example.my-api`
   - **Packaging**: Jar
   - **Java Version**: 21

3. Dans la section "Dependencies", ajoutez les dépendances suivantes :
   - **Spring Web** (pour créer l'API REST)
   - **Spring Data JPA** (pour la gestion des données avec JPA/Hibernate)
   - **PostgreSQL Driver** (pour se connecter à PostgreSQL)

4. Cliquez sur **Generate** pour télécharger le projet.

5. Extrayez le fichier ZIP et ouvrez-le dans votre IDE.
Vous devez maintenant avoir un répertoire "my-api" dans votre répertoire de travail.

6. On tente un premier package de l'application.
```sh
mvn package -DskipTests
```
Si une erreur du type "Le réseau n'est pas accessible" apparait exécuté les instruction suivante :
```sh
nano ~/.mavenrc
```
Ajouter la ligne suivante puis enregistrer.
```
MAVEN_OPTS="-Djava.net.preferIPv6Addresses=true"
```
Relancer la packaging de l'application pour vérifier le bon fonctionnement.

---

## Étape 2 : Configurer Spring Boot pour se connecter à PostgreSQL


1. Ouvrez le fichier src/main/resources/application.properties (ou application.yml).

2. Ajoutez les configurations suivantes pour la connexion à la base de données PostgreSQL :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```
- spring.jpa.hibernate.ddl-auto=update : permet à Hibernate de créer ou mettre à jour les tables automatiquement.
---
## Étape 3 : Créer une entité JPA

1. Créez un nouveau package com.example.my_api.model
2. Ajoutez une classe Product pour représenter une entité de produit
```java
package com.example.my_api.model;

import jakarta.persistence.*;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double price;

    // Constructeurs
    public Product() {}

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

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
Hint : Si vous ajoutez la dependance Lombok vous n'aurez plus a déclarer les constructeurs et les gettter/setter en ajoutant simplement l'annotation @Data sur la classe.

---
## Étape 4 : Créer un Repository

1. Créez un package com.example.my_api.repository.
2. Ajoutez une interface ProductRepository qui étend JpaRepository :
```java
package com.example.my_api.repository;

import com.example.my_api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```
- Question : A quoi sert cette classe ? 

---

## Étape 5 : Créer un Service

1. Créez un package com.example.my_api.service.
2. Ajoutez une classe ProductService pour gérer la logique métier de l'application :
```java
package com.example.my_api.service;

import com.example.my_api.model.Product;
import com.example.my_api.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
```

---

## Étape 6 : Créer un Controller

1. Créez un package com.example.my_api.controller.
2. Ajoutez une classe ProductController pour définir les endpoints REST :
```java
package com.example.my_api.controller;

import com.example.my_api.model.Product;
import com.example.my_api.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
```

---

## Étape 7 : Tester l'API

1. Lancez l'application en exécutant la classe MyApiApplication.java.
2. Utilisez un outil comme Postman ou cURL pour tester les endpoints.
Par exemple pour récupérer tous les produits :
- GET http://localhost:8080/api/products

Vous devriez voir une liste vide "[]" car aucun produit existe dans votre base de données.

---

## Étape 8 : A vous de jouer !

Compléter votre application pour obtenir les quatre opérations de base de persistance des données (CRUD).

Vous devez donc être capable :
- d'ajouter un produit dans la base de données
- de récupérer un seul produit par son ID
- de modifier un produit dans la base de données
- de supprimer un produit dans la base données

- Question : Alors que nous n'avons jamais crée de table dans la base de données, comment peut-on y stocker des données ? 

## Étape 9 : Pour les plus rapide du groupe

Etes-vous capable d'ajouter une colonne dans le modèle comme par exemple une date de fin de commercialisation et ainsi adapter les services CRUD.

Ajoutez également un service search pour obtenir la liste des produits disponible à une date donnée.

Vous obtiendrez ainsi une API avec les opérations du SCRUD !




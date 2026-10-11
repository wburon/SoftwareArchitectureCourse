# TP : Batch Spring pour ingérer des produits depuis un fichier CSV

## Objectif :
L'objectif est de créer une application Spring Boot Batch qui consomme un fichier csv et enregistrer les données dans une base de données. Nous utiliserons :
- **Spring Batch** : Cette dépendance fournit tous les outils nécessaires pour exécuter des jobs batch dans Spring.

---

## Étape 0 : Avant de démarrer

Ne suivez pas bêtement les étapes de ce TP ! Si vous estimez pouvoir arriver au même résultat plus simplement, lancez-vous !

Souhaitez-vous centraliser toutes les logiques dans une seule application ?
Souhaitez-vous avoir une séparation claire des responsabilités ?
Le batch doit-il être déclenché à la demande ?
Le batch doit-il être exécuté régulièrement ?
Le batch nécessite-t-il des ressources dédiées ou spécifiques (CPU, RAM) ?

## Étape 1 : Initialisation du projet Spring Boot

1. Rendez-vous sur [Spring Initializr](https://start.spring.io/). **Sur votre VM...**
2. Configurez le projet avec les paramètres suivants :
    - **Project** : Maven Project
    - **Language** : Java
    - **Spring Boot** : Version 4.1.1
    - **Group** : `com.example`
    - **Artifact** : `my-batch`
    - **Packaging** : Jar
    - **Java** : 21

3. Ajoutez les **dépendances** suivantes :
    - **Spring Batch** : Cette dépendance fournit tous les outils nécessaires pour exécuter des jobs batch dans Spring.
    - **Spring Data JPA** : Pour interagir avec la base de données à l’aide de JPA (Java Persistence API).
    - **PostgreSQL Driver** : Le pilote JDBC pour PostgreSQL, qui est nécessaire pour se connecter à votre base de données.

4. Cliquez sur **Generate** et extrayez le projet téléchargé dans votre environnement de développement (ex. IntelliJ IDEA, Eclipse).

---

## Étape 2 : Créer un modèle Product
L'entité Product correspond à la structure des données que vous allez importer dans votre base de données.
Cette classe représente un produit avec les champs id, name, et price. Elle est annotée avec @Entity pour indiquer que cette classe est une entité JPA qui sera stockée dans la base de données.

1. Créez un package **com.example.my-batch.model**.
2. Créez une classe **Product.java** :

```java
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Product {

    @Id
    private Long id;
    private String name;
    private double price;

    // Getters et Setters
}
```

## Étape 3 : Créer un repository
Ensuite, vous avez besoin d'un repository pour accéder aux données de la table Product.
L'interface JpaRepository offre toutes les fonctionnalités nécessaires pour interagir avec la base de données, comme l'ajout, la modification, et la récupération des entités Product.

1. Créez un package **com.example.my-batch.repository**.
2. Créez une classe **ProductRepository.java** :

```java
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

## Étape 4 : Créer la configuration Spring Batch

Vous allez maintenant configurer Spring Batch pour lire les produits à partir d’un fichier CSV, les traiter et les enregistrer dans la base de données.

Cette classe configure le processus de traitement des produits en plusieurs étapes :
- Reader : Lire le fichier CSV.
- Processor : Transformer les données (nous ne faisons aucune transformation dans cet exemple).
- Writer : Enregistrer les produits dans la base de données.

1. Créez un package **com.example.my-batch.config**.
2. Créez une classe **BatchConfiguration.java** :
```java
package com.example.my_batch.config;

import com.example.my_batch.model.Product;
import com.example.my_batch.repository.ProductRepository;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfiguration {

   private final ProductRepository productRepository;

   public BatchConfiguration(ProductRepository productRepository) {
      this.productRepository = productRepository;
   }

   // 1. Lecture du fichier CSV

   @Bean
   public FlatFileItemReader<Product> reader() {
      DelimitedLineTokenizer tokenizer =
              new DelimitedLineTokenizer();
      tokenizer.setNames("name", "price");

      BeanWrapperFieldSetMapper<Product> fieldSetMapper =
              new BeanWrapperFieldSetMapper<>();
      fieldSetMapper.setTargetType(Product.class);

      DefaultLineMapper<Product> lineMapper =
              new DefaultLineMapper<>();
      lineMapper.setLineTokenizer(tokenizer);
      lineMapper.setFieldSetMapper(fieldSetMapper);

      FlatFileItemReader<Product> reader =
              new FlatFileItemReader<>(
                      new ClassPathResource("products.csv"),
                      lineMapper
              );

      reader.setLinesToSkip(1);

      return reader;
   }


   // 2. Traitement de chaque produit
   @Bean
   public ItemProcessor<Product, Product> processor() {
      // TODO
   }

   // 3. Enregistrement des produits
   @Bean
   public ItemWriter<Product> writer() {
      // TODO
   }

   // 4. Étape du batch
   @Bean
   public Step productStep(
           JobRepository jobRepository,
           PlatformTransactionManager transactionManager) {

      return new StepBuilder("productStep", jobRepository)
              .<Product, Product>chunk(10)
              .transactionManager(transactionManager)
              .reader(reader())
              .processor(processor())
              .writer(writer())
              .build();
   }

   // 5. Définition du job
   @Bean
   public Job importProductJob(
           JobRepository jobRepository,
           Step productStep) {

      return new JobBuilder("importProductJob", jobRepository)
              .start(productStep)
              .build();
   }

   // 6. Lancement du job au démarrage
   @Bean
   public CommandLineRunner runBatchJob(
           JobOperator jobOperator,
           Job importProductJob) {

      return args -> {
         System.out.println("Starting the batch job...");

         var jobParameters = new JobParametersBuilder()
                 .addLong("run.timestamp",
                         System.currentTimeMillis())
                 .toJobParameters();

         var execution = jobOperator.start(
                 importProductJob,
                 jobParameters
         );

         System.out.println(
                 "Job started with execution ID: "
                         + execution.getId()
         );
      };
   }
}
```

## Étape 5 : Créer le fichier CSV

Créez un fichier CSV contenant les produits à importer. Le fichier products.csv doit être placé dans le dossier src/main/resources/ de votre projet.

Exemple de contenu du fichier products.csv :
```csv
name,price
Produit 1,10.0
Produit 2,20.0
Produit 3,30.0
```

## Étape 6 : Lancer l'application

1. Démarrez l'application
2. Vérifier dans postgres directement ou via un autre composant que les données ont bien été ajouté dans la base de données.

**Tout n'a pas du bien se passer ! A vous de jouer pour corriger !**

## Étape 7 : Batch inverse

Sauriez-vous maintenant réaliser le batch inverse ?
A savoir un batch qui génère un fichier csv avec les données présentes dans la base de données.
# TP7 --- Événementiel et asynchronisme avec le pattern Outbox

## Objectifs

Nous allons faire évoluer le projet existant `my-api` afin qu'il publie
un événement lorsqu'un produit est créé. Un second projet Spring Boot,
`product-consumer`, recevra cet événement de manière asynchrone.

Ce TP utilise les versions et conventions du dépôt fourni : **Java 21**,
**Spring Boot 4.1.1**, le package `com.example.my_api`, PostgreSQL et
Maven.

À la fin, le flux sera le suivant :

``` text
Client HTTP
    |
    v
my-api : ProductService
    |
    | une transaction PostgreSQL
    +------> product
    +------> outbox_event
                   |
                   v
            OutboxPublisher (@Scheduled)
                   |
                   v
                RabbitMQ
                   |
                   v
         product-consumer (@RabbitListener)
```

Le publisher interroge périodiquement l'Outbox : cette partie ressemble
à du polling. En revanche, le consommateur reçoit les messages via
RabbitMQ, sans devoir interroger `my-api`.

## Étape 0 --- Comprendre le projet existant

Le projet `my-api` contient déjà les classes suivantes :

-   `com.example.my_api.model.Product`
-   `com.example.my_api.repository.ProductRepository`
-   `com.example.my_api.service.ProductService`
-   `com.example.my_api.controller.ProductController`

L'API expose notamment `POST /api/products`. **Ne remplacez pas le
contrôleur et le modèle Product** : nous allons principalement faire
évoluer le service et ajouter de nouvelles classes.

Avant de coder, réfléchissez aux questions suivantes :

1.  Quelle différence faites-vous entre une requête de création de
    produit et un événement `ProductCreated` ?
2.  Que se passe-t-il si le produit est enregistré en base mais que
    RabbitMQ est indisponible ?
3.  Pourquoi ne suffit-il pas d'appeler directement RabbitMQ après
    `productRepository.save(...)` ?
4.  Pourquoi un consommateur doit-il tolérer la réception d'un même
    événement plusieurs fois ?

## Étape 1 --- Démarrer RabbitMQ

À la racine du projet, créez `docker-compose.yml` :

``` yaml
services:
  rabbitmq:
    image: rabbitmq:4-management
    container_name: tp-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
```

Démarrez RabbitMQ :

``` bash
docker compose up -d
```

L'interface de gestion est disponible sur `http://localhost:15672`. Avec
cette configuration de démonstration, les identifiants par défaut sont
généralement `guest` / `guest`.

Vérifiez que le conteneur fonctionne avant de poursuivre.

## Étape 2 --- Ajouter les dépendances à `my-api`

Dans `my-api/pom.xml`, ajoutez la dépendance Spring AMQP dans
`<dependencies>` :

``` xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

Ne supprimez pas les dépendances déjà présentes. La version est gérée
par le parent Spring Boot du projet.

Dans `my-api/src/main/resources/application.properties`, conservez les
propriétés PostgreSQL et ajoutez :

``` properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Demande à RabbitMQ de confirmer la réception des messages publiés
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
```

Redémarrez Maven / rechargez le projet dans l'IDE afin que la dépendance
soit prise en compte.

## Étape 3 --- Définir le contrat de l'événement

Créez le package `com.example.my_api.event`, puis le fichier
`ProductCreated.java`.

**Classe complète :
`my-api/src/main/java/com/example/my_api/event/ProductCreated.java`**

``` java
package com.example.my_api.event;

import java.time.Instant;
import java.util.UUID;

public record ProductCreated(
        UUID eventId,
        Long productId,
        String name,
        double price,
        Instant occurredAt
) {
}
```

`eventId` identifie l'événement ; `productId` identifie le produit. Ces
identifiants ne répondent donc pas au même besoin.

Nous publierons un contrat d'événement dédié plutôt que l'entité JPA
`Product`. Cela évite de coupler le message aux détails de persistance
de l'API.

## Étape 4 --- Créer la table Outbox

La table Outbox conserve l'événement à publier dans la même transaction
que le produit. Nous utilisons ici une colonne `TEXT` contenant du JSON
: cela évite de dépendre d'un mapping spécifique de type JSONB dans
Hibernate.

Exécutez dans la base PostgreSQL utilisée par `my-api` :

``` sql
CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_event_status_created
    ON outbox_event (status, created_at);
```

### 4.1. Créer l'entité `OutboxEvent`

Compléter le package `com.example.my_api.model`.

**Classe complète :
`my-api/src/main/java/com/example/my_api/model/OutboxEvent.java`**

``` java
package com.example.my_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public OutboxEvent() {
    }

    public OutboxEvent(
            UUID id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            String status,
            Instant createdAt,
            Instant publishedAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
```

### 4.2. Créer le repository Outbox

**Classe complète :
`my-api/src/main/java/com/example/my_api/repository/OutboxEventRepository.java`**

``` java
package com.example.my_api.repository;

import com.example.my_api.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop5ByStatusOrderByCreatedAtAsc(
            String status);
}
```

La méthode dérivée récupère jusqu'à 5 événements dans l'ordre
chronologique. Le publisher l'appellera avec le statut `PENDING`.

## Étape 5 --- Enregistrer produit et événement dans la même transaction

### 5.1. Comprendre le changement attendu

Lors d'une création, `ProductService` doit :

1.  enregistrer le produit ;
2.  construire un événement `ProductCreated` ;
3.  sérialiser l'événement en JSON ;
4.  enregistrer l'événement avec le statut `PENDING` ;
5.  valider les deux écritures dans une seule transaction.

Si l'une des écritures échoue, la transaction doit annuler les deux
opérations.

### 5.2. Completer `ProductService`

Completer `addProduct` comme ceci.

``` java
    @Transactional
    public Product addProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        ProductCreated event = new ProductCreated(
                UUID.randomUUID(),
                savedProduct.getId(),
                savedProduct.getName(),
                savedProduct.getPrice(),
                Instant.now()
        );

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Impossible de sérialiser l'événement ProductCreated",
                    exception
            );
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                event.eventId(),
                "Product",
                savedProduct.getId().toString(),
                "ProductCreated",
                payload,
                "PENDING",
                Instant.now(),
                null
        );

        outboxEventRepository.save(outboxEvent);

        return savedProduct;
    }
```

En ajoutant outboxEventRepository et objectMapper dans le constructeur de ProductService.

`ObjectMapper` est fourni automatiquement par Spring Boot. L'exception
de sérialisation est convertie en exception d'exécution : celle-ci
provoque le rollback de la transaction.

Le contrôleur `ProductController` et le modèle `Product` n'ont pas
besoin d'être modifiés.

### 5.3. Tester le fonctionnement nominal et le rollback

1.  Démarrez `my-api`.
2.  Envoyez une requête `POST /api/products`, par exemple :

``` http
POST http://localhost:8080/api/products
Content-Type: application/json

{
  "name": "Clavier",
  "price": 49.99
}
```

3.  Vérifiez que le produit a été enregistré dans `product`.
4.  Vérifiez qu'une ligne `PENDING` a été créée dans `outbox_event`.

Pour tester le rollback, ajoutez **temporairement** dans
`ProductService.addProduct`, juste après ou juste avant `outboxEventRepository.save(outboxEvent)` :

``` java
throw new RuntimeException("Erreur volontaire pour tester le rollback");
```

Relancez une requête de création : la réponse doit signaler une erreur
et ni le produit ni l'événement de cette tentative ne doivent être
validés. Supprimez ensuite cette ligne de test.

**Question :** pourquoi est-il important que les deux écritures
utilisent la même transaction PostgreSQL ?

## Étape 6 --- Déclarer la file RabbitMQ

Créez le package `com.example.my_api.config`.

**Classe complète :
`my-api/src/main/java/com/example/my_api/config/RabbitMQConfiguration.java`**

``` java
package com.example.my_api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    @Bean
    public Queue productCreatedQueue() {
        return new Queue("product.created", true);
    }
}
```

La queue est durable (true). Au démarrage, Spring AMQP demande à RabbitMQ de
déclarer cette queue.

Démarrez RabbitMQ puis `my-api`. Dans l'interface
`http://localhost:15672`, ouvrez **Queues and Streams** et vérifiez la
présence de `product.created`.

## Étape 7 --- Publier les événements Outbox

Le publisher vérifie périodiquement la table. Il ne marque un événement
`PUBLISHED` qu'après confirmation du broker.

### 7.1. Activer la planification

Ajoutez l'annotation @EnableScheduling dans `my-api/src/main/java/com/example/my_api/MyApiApplication.java`.

### 7.2. Créer `OutboxPublisher`

**Classe complète :
`my-api/src/main/java/com/example/my_api/service/OutboxPublisher.java`**

``` java
package com.example.my_api.service;

import com.example.my_api.model.OutboxEvent;
import com.example.my_api.repository.OutboxEventRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository
                        .findTop5ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : pendingEvents) {
            try {
                publishOne(event);
            } catch (Exception exception) {
                // L'événement reste PENDING : un prochain passage réessaiera.
                System.err.println(
                        "Échec de publication de l'événement "
                                + event.getId() + " : "
                                + exception.getMessage()
                );
            }
        }
    }

    private void publishOne(OutboxEvent event) throws Exception {
        CorrelationData correlationData =
                new CorrelationData(event.getId().toString());

        // Le payload est déjà un document JSON sérialisé.
        // On l'envoie comme String ; le consommateur le désérialisera.
        rabbitTemplate.convertAndSend(
                "",
                "product.created",
                event.getPayload(),
                correlationData
        );

        CorrelationData.Confirm confirm =
                correlationData.getFuture().get(5, TimeUnit.SECONDS);

        if (!confirm.isAck()) {
            throw new IllegalStateException(
                    "RabbitMQ n'a pas confirmé le message : "
                            + confirm.getReason()
            );
        }

        // Enregistrer l'état après confirmation du broker.
        event.setStatus("PUBLISHED");
        event.setPublishedAt(Instant.now());
        outboxEventRepository.save(event);
    }
}
```

Le premier argument de `convertAndSend` est `""` : c'est le **default
exchange** de RabbitMQ. 
Dans RabbitMQ, le default exchange est un exchange particulier qui permet d'envoyer directement un message vers une queue en utilisant le nom de cette queue comme clé de routage (routing key).
Le routing key `product.created` correspond au
nom de la queue, qui est automatiquement liée au default exchange.

La configuration `spring.rabbitmq.publisher-confirm-type=correlated`
ajoutée à l'étape 2 permet d'attendre une confirmation corrélée à la
publication.

> **Limite à connaître :** cette version est adaptée au TP, mais ne gère
> pas toutes les situations de production. Un message peut être confirmé
> par RabbitMQ puis l'application s'arrêter avant d'enregistrer
> `PUBLISHED`. L'événement sera alors republié au redémarrage. Le
> consommateur doit donc être capable de gérer les doublons. De plus,
> une confirmation positive ne prouve pas que le consommateur a traité
> le message.

### 7.3. Vérifier la publication

1.  Démarrez RabbitMQ et `my-api`.
2.  Créez un produit.
3.  Vérifiez que la ligne apparaît d'abord en `PENDING`.
4.  Attendez quelques secondes.
5.  Vérifiez que son statut passe à `PUBLISHED`.
6.  Dans RabbitMQ, ouvrez `product.created` et vérifiez qu'un message
    est disponible.

Requête utile :

``` sql
SELECT id, event_type, status, created_at, published_at
FROM outbox_event
ORDER BY created_at DESC;
```

## Étape 8 --- Créer le consommateur `product-consumer`

Créez un **second projet Maven Spring Boot**, séparé de `my-api`, avec
Java 21 et Spring Boot 4.1.1. Ajoutez les dépendances Spring AMQP et
Spring Web MVC (cette dernière fournit notamment Jackson pour
désérialiser le JSON).

### 8.1. Configuration

Dans `product-consumer/src/main/resources/application.properties` :

``` properties
spring.application.name=product-consumer
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 8.3. Recréer le contrat d'événement dans le consommateur

Les deux applications doivent partager le même contrat. Pour ce TP, nous
recopions le `record` dans le second projet. Dans un projet plus grand,
on pourrait extraire le contrat dans une bibliothèque Maven partagée.

**Classe complète :
`product-consumer/src/main/java/com/example/product_consumer/event/ProductCreated.java`**

``` java
package com.example.product_consumer.event;

import java.time.Instant;
import java.util.UUID;

public record ProductCreated(
        UUID eventId,
        Long productId,
        String name,
        double price,
        Instant occurredAt
) {
}
```

### 8.4. Écouter la queue

Le publisher envoie une chaîne contenant le JSON. Le consommateur la
reçoit comme `String`, puis utilise Jackson pour la convertir en
`ProductCreated`.

**Classe complète :
`product-consumer/src/main/java/com/example/product_consumer/listener/ProductCreatedListener.java`**

``` java
package com.example.product_consumer.listener;

import com.example.product_consumer.event.ProductCreated;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductCreatedListener {

    private final ObjectMapper objectMapper;

    public ProductCreatedListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "product.created")
    public void onMessage(String json) throws Exception {
        ProductCreated event =
                objectMapper.readValue(json, ProductCreated.class);

        System.out.println("Événement reçu : " + event.eventId());
        System.out.println("Produit : " + event.productId()
                + " — " + event.name()
                + " — " + event.price());

        // Simulation d'un traitement métier lent.
        Thread.sleep(3000);

        System.out.println("Traitement terminé pour "
                + event.eventId());
    }
}
```

Le `throws Exception` est volontairement simple pour l'exercice. Dans
une application réelle, il faut définir une stratégie de gestion des
erreurs et des tentatives de livraison.

### 8.5. Tester l'asynchronisme

1.  Démarrez RabbitMQ.
2.  Démarrez `my-api`.
3.  Démarrez `product-consumer`.
4.  Créez plusieurs produits via `POST /api/products`.
5.  Comparez les logs de `my-api` et ceux du consommateur.

La réponse HTTP ne doit pas attendre la fin du `Thread.sleep(3000)` du
consommateur : le service de création enregistre le produit et
l'événement, puis le publisher relaie le message séparément.

## Étape 9 --- Tester une panne

### 9.1. Arrêter RabbitMQ après le démarrage des applications

1.  Vérifiez que `my-api` fonctionne.
2.  Arrêtez RabbitMQ :

``` bash
docker compose stop rabbitmq
```

3.  Créez un produit.
4.  Vérifiez que le produit et son événement Outbox ont été enregistrés.
5.  Le publisher ne peut pas publier : l'événement doit rester
    `PENDING`.
6.  Redémarrez RabbitMQ :

``` bash
docker compose start rabbitmq
```

7.  Attendez le passage suivant du publisher et vérifiez que l'événement
    passe à `PUBLISHED`.

Ce test consiste à arrêter RabbitMQ **après le démarrage de `my-api`**.
La déclaration automatique de la queue peut nécessiter que RabbitMQ soit
disponible au démarrage initial.

### 9.2. Comprendre le risque de doublon

Scénario possible :

1.  RabbitMQ reçoit le message et confirme sa réception.
2.  `my-api` s'arrête avant d'enregistrer `PUBLISHED`.
3.  Au redémarrage, le publisher retrouve l'événement `PENDING` et le
    publie à nouveau.

L'Outbox évite de perdre l'intention de publier entre l'écriture métier
et la publication, mais ne garantit pas une livraison exactement une
fois. Une étape complémentaire consiste à rendre le consommateur
idempotent, en mémorisant durablement les `eventId` déjà traités et en
protégeant le traitement métier par une transaction.

**À vous de jouer :** proposez une table `processed_event` avec une
contrainte d'unicité sur `event_id`. Expliquez comment combiner
l'enregistrement de cet identifiant et le traitement métier dans la même
transaction.

# TP ELK : Analyse de logs d'API REST avec le dataset LO2

---

## 🎯 Objectifs pédagogiques

- Comprendre le rôle des composants de la stack **ELK** (Elasticsearch, Logstash, Kibana).
- Déployer un environnement ELK fonctionnel avec **Docker Compose**.
- Ingestion et exploration de **logs d’API REST** issus du dataset **LO2**.
- Comprendre la différence entre **logs bruts** et **logs structurés** (avec parsing `grok`).
- Créer des visualisations et tableaux de bord dans Kibana.

---

## 🧠 Contexte

Nous allons analyser des **logs d’un système de microservices exposant des API REST**, extraits du dataset :
>  **LO2 – Microservice API Anomaly Dataset of Logs and Metrics**
> Source : [https://arxiv.org/abs/2504.12067](https://arxiv.org/abs/2504.12067)

Ce dataset simule une architecture de microservices avec :
- Des appels HTTP (`GET`, `POST`, `PUT`, `DELETE`)
- Des codes de statut (`2xx`, `4xx`, `5xx`)
- Des temps de réponse
- Des anomalies (`is_anomaly`)

---

## 🛠️ Prérequis

- Machine avec **≥ 4 Go RAM libres**
- **Docker** et **Docker Compose** installés
- Connaissance basique de Linux et JSON
- Dataset LO2 téléchargé (ou échantillon fourni)

---

## 🔢 Partie 1 – Structure du projet et déploiement ELK

### 1.1 – Arborescence du projet

tp-elk-lo2/
│
├── docker-compose.yml
├── pipeline/
│ ├── logstash_raw.conf
│ └── logstash_parsed.conf
├── data/
│ └── input_raw.log

### 1.2 – Contenu du repo GitHub

Cloner le repo GitHub : https://github.com/wburon/TP_ELK

```sh
git clone https://github.com/wburon/TP_ELK.git
```

Voici l'explication des fichiers que vous avez téléchargé.
**Il n'est donc pas nécessaire de les recopier. L'objectif est de comprendre la base du TP.**

#### 1️⃣ docker-compose.yml

**Rôle :** Décrit et démarre l’ensemble de la stack ELK avec Docker.
**Services :**
| Service | Image | Rôle |
| ------------- | -------------------- | ------------------------------------------------------------------------------ |
| elasticsearch | elasticsearch:8.15.0 | Stockage et recherche des logs. |
| kibana | kibana:8.15.0 | Interface web pour explorer les logs et créer des dashboards. |
| logstash | logstash:8.15.0 | Pipeline de traitement et transformation des logs avant envoi à Elasticsearch. |

**Volumes :**

* `./pipeline:/usr/share/logstash/pipeline` → Contient les fichiers `.conf` de Logstash.
* `./data:/usr/share/logstash/data` → Contient les logs bruts (`input_raw.log`) et doit être **écriture-permise**.
* `es_data` → Volume Docker pour Elasticsearch, persiste les données.

```yaml
version: "3.8"

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
    volumes:
      - es_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    networks:
      - elk

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.0
    container_name: kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - SERVER_NAME=kibana
    depends_on:
      - elasticsearch
    ports:
      - "5601:5601"
    networks:
      - elk

  logstash:
    image: docker.elastic.co/logstash/logstash:8.15.0
    container_name: logstash
    environment:
      - LS_JAVA_OPTS=-Xms512m -Xmx512m
    volumes:
      - ./pipeline:/usr/share/logstash/pipeline
      - ./data:/usr/share/logstash/data
    depends_on:
      - elasticsearch
    ports:
      - "5044:5044"
      - "9600:9600"
    networks:
      - elk

networks:
  elk:
    driver: bridge

volumes:
  es_data:
```

---

#### 2️⃣ pipeline/ (dossier)

Contient les **pipelines Logstash** pour traiter les logs. Chaque fichier `.conf` est un pipeline séparé.

Un pipeline Logstash est une chaîne de traitements qui prend des données en entrée, les transforme (au besoin), puis les envoie en sortie vers une destination.

#### a) logstash_raw.conf

**Rôle :** Ingestion brute des logs sans parsing.
**Explications :**
* Lit les logs depuis le fichier local.
* Envoie les logs tels quels dans Elasticsearch (`lo2-raw`).
* Les logs restent **non structurés**, tout est dans le champ `message`.

#### b) logstash_parsed.conf

**Rôle :** Ingestion avec parsing pour extraire des champs exploitables.
**Explications :**
* `grok` transforme la ligne de log brute en **champs structurés**.
* `date` convertit le champ `timestamp` en format Elasticsearch utilisable pour les visualisations.
* `stdout` affiche les logs transformés dans la console pour debug.

---

#### 3️⃣ data/input_raw.log

**Rôle :** Contient les **logs simulés** à ingérer dans ELK.
**Exemple de contenu :**

```
2024-03-01T12:00:15.452Z [INFO] order_service POST /api/orders 200 123ms
2024-03-01T12:00:16.012Z [ERROR] payment_service POST /api/payments 500 321ms
2024-03-01T12:00:17.402Z [INFO] auth_service GET /api/users 200 98ms
```

### 1.3 – Démarrage de la stack

Lancez les conteneurs Docker :

```sh
sudo docker compose up -d
```

Vérifiez que les services tournent :

```sh
sudo docker ps
```

Vous devez voir :
- `elasticsearch`
- `kibana`
- `logstash`

Accès :
- **Elasticsearch** : [http://localhost:9200](http://localhost:9200/)
- **Kibana** : [http://localhost:5601](http://localhost:5601/)

---

## 🔢 Partie 2 – Observation dans Kibana

- Ouvrir Kibana → **Stack Management → Data Views → Create data view**
- Nommer : `lo2-raw`
- Ouvrir **Discover**

🔎 **Constat :** Les logs sont visibles, mais tout est contenu dans un seul champ `message`. Impossible de filtrer ou agréger les données.

----------

## 3️⃣ Partie 3 – Parsing des logs avec `grok`

Grok repose sur des **expressions régulières (regex)** et des modèles prédéfinis pour analyser des textes. Ces modèles sont utilisés pour matcher des motifs communs dans les logs et **extraire des parties spécifiques de chaque ligne de log**.

### 3.1 – Création du pipeline structuré

Créez `pipeline/logstash_parsed.conf` :

⚠️ **NE PAS LAISSER LE COMMENTAIRE DANS LE FICHIER** ⚠️

```conf
input {
  file {
    path => "/usr/share/logstash/data/input_raw.log"
    start_position => "beginning"
    sincedb_path => "/dev/null" # À ajouter uniquement si la lecture du fichier est KO
  }
}

filter {
  grok {
    match => {
      "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{LOGLEVEL:level}\] %{WORD:service} %{WORD:method} %{URIPATH:endpoint} %{NUMBER:status:int} %{NUMBER:response_time_ms:int}ms"
    }
  }

  date {
    match => ["timestamp", "ISO8601"]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "lo2-parsed"
  }

  stdout {
    codec => rubydebug
  }
}
```



Redémarrer uniquement logstash pour être certain de prendre en compte ce nouveau pipeline.

```sh
sudo docker compose restart logstash
```

### 3.2 – Comparaison dans Kibana

Créez une nouvelle Date Views : `lo2-parsed`.

Saurez-vous visualiser les logs dans leur nouveau format dans Kibana ?

indice : Discover

----------

## 4️⃣ Partie 4 – Visualisations et tableaux de bord

### 4.1 – Création de visualisations

Dans Kibana → **Visualize Library**, créez les visualisations suivantes en utilisant **Lens**:

1. **Histogramme** : nombre de requêtes par minute
- Axe X : `timestamp`
- Axe Y : `count()`

2. **Camembert** : répartition des codes HTTP
- Split by : `status`

3. **Bar chart** : temps de réponse moyen par service
- Axe X : `service`
- Metric : `avg(response_time_ms)`

### 4.2 – Création d’un tableau de bord

Créez un **dashboard** intitulé :

>  `Analyse API REST – LO2`

Ajoutez-y vos trois visualisations.

Ajoutez un filtre `status >= 400` pour ne garder que les requêtes en erreur.

----------

# Docker - TP2 : Construction d'images et Registres
> **Objectifs du TP** :
>- Utiliser un registre afin de stocker des images Docker
>- Comprendre comment est construite une image Docker avec son application
>
> **Niveau de difficulté** : Débutant

## 1- Les Images Docker et le Registre distant (Docker Hub)

Nous avons vu dans la première partie que pour lancer un conteneur, nous avons **besoin** d'une image. Il s’agit d’un modèle qui nous permet de créer notre conteneur, en d'autres termes tout ce qu'il contiendra en termes de fichiers et de programmes lors de son lancement. Dans nos exemples, nous avons créé des conteneurs utilisant une image de base Ubuntu.

Comme nous l'avons vu dans le cours, une image est composée de plusieurs couches en lecture seule. Cela permet de créer une hiérarchie réutilisable par tous les conteneurs. Si vous lancez deux conteneurs avec pour image de base Ubuntu, vous n'aurez pas deux fois 400 Mo de fichiers utilisés (comme pour des machines virtuelles), mais bien une fois 400 Mo.

Docker utilise overlay2 ou d'autres systèmes de fichiers comme overlay, BTRFS (comme on peut le voir avec la commande `docker info`) comme système de fichiers. C'est un système de fichiers unifié (_union mount_), qui permet de fusionner plusieurs systèmes de fichiers distincts et de les faire apparaître comme un unique système de fichiers avec une hiérarchie unifiée.

Ainsi plusieurs conteneurs peuvent monter leur système de fichiers avec des briques leur appartenant et d'autres partagées entre tous les conteneurs. Si un de ces conteneurs écrit dans un fichier, il va créer une copie qui lui est propre afin de pouvoir l’utiliser localement après cette modification (mécanisme de _copy on write_).

Les images sont des éléments réutilisables et il en existe pour beaucoup de variétés d'environnements (Java, Python, MySQL, Oracle DB, Postgres, Node.js, etc.). Afin de partager ces environnements, on utilise un **registre** (registry dans le vocabulaire Docker) dans lequel on va stocker ces modèles (et toute la structure de fichiers associée). Imaginez ce registre distant comme un ensemble de dépôts Subversion ou Git. De la même manière qu'on stocke du code, on stocke ici des briques de base constituant nos environnements afin de les réutiliser.

## 2- Lister les images docker

Commençons par lister les images disponibles pour notre démon Docker. Les images listées sont celles qui sont directement disponibles localement afin de construire et lancer un conteneur.
```sh
docker image ls
```
```text
REPOSITORY  TAG     IMAGE ID      CREATED      SIZE
ubuntu      latest  f753707788c5  4 weeks ago  127.2 MB
```
Les images qui ne sont pas listées doivent être téléchargées à partir du Docker Hub, qui est le registre hébergeant toutes les images de base mises à disposition par Docker et la communauté. N'importe qui peut créer un compte sur le Docker Hub et y stocker des images.

Les images sont stockées dans des dépôts (à la manière d'un repository SVN ou Git) qui sont eux-mêmes stockés dans un registre.
## 3- Télécharger une image Docker

Les images listées par la commande `docker images` ne font pas état d'une image alpine sur notre système.  Tentons donc de la télécharger à partir du Docker Hub. Pour ça, on utilise la commande `docker image pull`. Vous devriez observer la sortie suivante :
```sh
docker image pull alpine:3.24.2
```

>- Quelle version de l'image alpine est récupéré ?


Lançons un conteneur avec une image de base `alpine`.
```sh
docker container run -i -t alpine /bin/sh
```
On obtient ainsi un conteneur avec un système de fichiers et des programmes correspondant à la dernière distribution alpine. Depuis l’intérieur du conteneur, vérifions quelle version de alpine a été démarrée avec la commande `cat /etc/os-release`.
```
# cat /etc/os-release
NAME="Alpine Linux"
ID=alpine
VERSION_ID=3.8.0
PRETTY_NAME="Alpine Linux v3.8"
HOME_URL="http://alpinelinux.org"
BUG_REPORT_URL="http://bugs.alpinelinux.org"
```
Sortez maintenant de ce conteneur en tapant la commande `exit`.

Si on jette de nouveau un coup d'œil sur la sortie de la commande `docker image ls`, on remarque que deux images possèdent le même identifiant ! Comment cela peut-il être possible ? Une image n'est-elle pas censée posséder un identifiant unique ?
```sh
dev $ docker image ls
REPOSITORY                                               TAG       IMAGE ID
[...]
alpine                                                   3.5       a2b04ae28915
alpine                                                   3.4       993b1b41569d
alpine                                                   3.3       19bf2ec565c3
alpine                                                   3.2       e45221eb7f87
alpine                                                   3.1       6f8a01c2945d
alpine                                                   edge      9d1f27787d39
alpine                                                   3.8       11cd0b38bc3c
alpine                                                   latest    11cd0b38bc3c
alpine                                                   3.7       791c3e2ebfcb
alpine                                                   3.6       da579b235e92
[...]
```
En réalité, Docker utilise également des tags pour qualifier les images. Dans notre cas, les images avec les tags `3.24.2` et `latest` sont en réalité la **même image**. Par défaut, si on met juste `alpine`, Docker construit un nouveau conteneur avec la version marquée par un tag `latest`.

Pour choisir une autre distribution de alpine, par exemple `3.5`, il faut également spécifier le tag de l'image pour la commande docker container run.
```sh
docker container run -i -t alpine:3.5 /bin/sh
```
Encore une fois, depuis l’intérieur du conteneur, vérifions quelle version de alpine a été démarrée avec la commande `cat /etc/os-release`.
```
# cat /etc/os-release
NAME="Alpine Linux"
ID=alpine
VERSION_ID=3.5.2
PRETTY_NAME="Alpine Linux v3.5"
HOME_URL="http://alpinelinux.org"
BUG_REPORT_URL="http://bugs.alpinelinux.org"
```
Et voilà ! Nous avons cette fois-ci crée un conteneur avec comme image de base une distribution alpine 3.5. Sortez maintenant de ce conteneur en tapant la commande exit.

## 4- Chercher des images

Pour chercher des images disponibles dans le Docker Hub, on peut utiliser la commande `docker search`. Tentons de chercher le terme “mysql” :
```sh
docker search mysql
```
Vous devriez observer en sortie une grosse quantité de dépôts dans le Docker Hub. Pour chacun d'eux, on peut observer de nombreuses informations comme le nom, la description, les "stars" et les indicateurs "OFFICIAL" et "AUTOMATED".


Le nom identifie l'image. Comme on peut le remarquer, beaucoup de noms sont sous la forme `<namespace>/<nom_repository>` et représentent les dépôts qui sont fournis par la communauté d'utilisateurs de Docker. D'autres sont seulement sous la forme `<nom_repository>` et correspondent aux repositories contenant des images officielles et supportées par l'équipe Docker.

Les Stars montrent la popularité d'une image. Plus elle en a et plus elle est populaire au sein de la communauté. L'indicateur “OFFICIAL” nous indique si l'image est une image officielle et “AUTOMATED” nous informe si l'image a été construite à partir d’un outil de build automatique du Docker Hub (mais nous y reviendrons plus tard. Ce n’est pas la peine de retenir ces informations pour le moment).


A titre d'exemple, téléchargeons une image officielle.
```sh
docker image pull mysql
```
Puis:
```sh
docker container run -i -t --name=mysql mysql /bin/bash
```
Puis à l'intérieur du conteneur :
```
# mysql --version
```
La commande nous renvoie bien la version de MySQL installée. Nous n'avons pas besoin de lancer apt-get ou un autre gestionnaire de paquet, MySQL est déjà présent dans notre image.

Sortez maintenant du conteneur en lançant la commande `exit`.

## 5- Supprimer une image

Pour supprimer une image localement sur notre machine, on utilise la commande `docker image rm`.

Par défaut, Docker ne veut pas supprimer une image qui est liée à un conteneur existant que celui-ci soit arrêté ou non. Il faut donc d’abord supprimer le conteneur mysql.
```sh
docker container rm mysql
```
On peut ensuite supprimer l’image :
```sh
docker image rm mysql
```
Observez la sortie : on remarque que chacune des couches du système de fichiers sont supprimées les unes après les autres.

## 6- Construction de notre propre image

Maintenant que nous avons vu comment récupérer des images toutes prêtes avec des programmes préinstallés, créons nous même notre propre image personnalisée !
Il existe deux façons de créer notre propre image :

- Grâce à la commande `docker container commit` qui va créer une nouvelle couche de système de fichiers avec les nouveaux fichiers d'un conteneur (par exemple on installe MySQL au dessus d'une image de base ubuntu).

- Avec la commande `docker image build`. Cette commande est différente car une image est construite via une description dans un fichier appelé Dockerfile qui contient une suite d'instruction à exécuter pour construire une image. Par exemple, on peut décrire dans le fichier que l'on doit installer MySQL à partir d'une image de base Ubuntu mais ce n'est pas nous qui interagissons directement avec un conteneur et sauvegardons les changements. C'est Docker lui même qui se charge d'installer MySQL pour nous et qui sauvegarde une nouvelle image !

## 7- Utilisation d’un registre pour faire un commit des changements d'un conteneur

Nous allons maintenant faire un **commit** des changements d'un conteneur que l'on va lancer grâce à l'instruction `docker container commit`. Lançons un conteneur Ubuntu classique.
```sh
docker container run --name=vimubuntu -i -t ubuntu /bin/bash
```
Vous devriez obtenir, comme d’habitude, une invite de commande de la forme suivante :
```
root@adf131ef9a23:/#
```
Votre conteneur peut être identifié soit par le nom que vous lui avez donné au travers du flag `--name` ou grâce au nombre hexadecimal après root@, ici adf131ef9a23 : c’est l’identifiant court du conteneur.
Effectuons maintenant quelques changements dans notre conteneur.
```
# apt update
# apt -y install vim
```
Nous venons d'installer vim sur notre conteneur Ubuntu. Cependant, nous savons que si nous tuons le conteneur et que nous le supprimons, nous n'aurons aucun moyen de repartir de ce point sans avoir à retaper ces commandes et télécharger de nouveau le paquet Vim. Pour éviter cela, nous allons faire un commit du conteneur et pousser son image dans un dépôt de notre registre privé ! Tapez exit puis ensuite aller créer un compte sur Docker Hub : https://app.docker.com/

Ensuite, nous devons établir la connexion entre le terminal du Lab et votre compte Docker Hub.
```sh
docker login -u <username>
```

Nous pouvons ensuite commit les changements de notre image et poursuivre.
```sh
docker container commit vimubuntu <username>/vimubuntu
```

En retour de la commande, Docker nous donne un identifiant :
```text
sha256:f22ba10fe40b9d87fb51dfaf7a0e4d5e863e6a3eb6edb1b2feb0516f82ad3443
```
C'est l'identifiant de l'image que nous avons construite à partir des changements effectués dans le conteneur.

Pour vérifier que nous avons bien créé une nouvelle image, supprimons notre ancien conteneur.
```sh
docker container rm vimubuntu
```
Listons les images pour essayer de retrouver la nôtre:
```sh
docker image ls
```
```text
REPOSITORY                     TAG       IMAGE ID            CREATED             SIZE
<username>/vimubuntu   latest    fe12ceb6e74c        8 minutes ago       186 MB
```
Notre image est bien listée comme <username>/vimubuntu (où les variables sont remplacées par les valeurs correspondantes à votre environnement). Nous pouvons maintenant l’envoyer au registre avec la commande suivante :
```sh
docker image push <username>/vimubuntu
```

Vous pouvez vérifier sur le Doker Hub que votre image est bien disponible.

Relançons un autre conteneur grâce à cette image :
```sh
docker container run -i -t <username>/vimubuntu /bin/bash
```
Tapez la commande vim dans le shell. On remarque que vim est déjà installé sur le conteneur. Nous avons sauvegardé nos changements avec succès!

Vous pouvez quitter vim en tapant sur la touche ESC puis en tapant :q! puis sortir du conteneur en lançant la commande exit.

## 8- Création d'une image à l'aide d'un Dockerfile

Nous savons que deux méthodes sont possibles pour construire une image. La deuxième, que nous allons voir maintenant, est la méthode recommandée. Nous allons décrire comment réaliser un petit fichier permettant de construire une image à l'aide d'une suite d'instructions. Ce fichier s'appelle le Dockerfile.

Nous allons créer notre propre application, et cette fois-ci à l'aide d'un Dockerfile !

Veillez à quitter le conteneur dans lequel vous êtes. Puis préparez à travailler dans le répertoire de travail `/workspace/app/`. Nous allons être amenés à créer plusieurs fichiers dans ce répertoire :

Dans le fichier `app.py`, copiez le code suivant (attention à l’indentation) :
```py
# app.py
from flask import Flask
from http import HTTPStatus
from werkzeug.exceptions import HTTPException

app = Flask(__name__)

@app.route("/")
def hello():
    return "Hello Docker!\n"

@app.errorhandler(HTTPException)
def handle_http_exception(exception: HTTPException):
    return exception.description, exception.code

@app.errorhandler(Exception)
def handle_exception(exception: Exception):
    return HTTPStatus.INTERNAL_SERVER_ERROR.description, HTTPStatus.INTERNAL_SERVER_ERROR
```

Ce code représente une application qui utilise un framework Python qui s’appelle `Flask`. Il nous permettra d'ajouter des fonctionnalités de façon assez simple, sans avoir besoin de rajouter beaucoup de code.

Pour l’instant l’application est très basique : elle répond sur la route racine `/` par `Hello Docker` et un code `HTTP 200`.

Pour s'exécuter, l'application nécessite des modules comme `Flask`. On utilisera aussi `gunicorn`, un serveur Python WSGI HTTP. Nous allons mettre toutes ces dépendances dans un fichier de prérequis standard en Python : `requirements.txt`. Dans ce fichier, mettez le contenu suivant :
```sh
# requirements.txt
Flask==2.3.3
gunicorn==22.0.0
```

Éditez également le fichier `config.py` avec le contenu suivant :
```py
# config.py
import os

bind = "0.0.0.0:8000"
worker_class = "gthread"
threads = int(os.getenv('GUNICORN_CORES','1')) * 2 + 1
```
Utilisez l'éditeur vim (ou autre) pour créer un fichier Dockerfile. Dans ce fichier, ajoutez le contenu suivant :

```Dockerfile
# Dockerfile
FROM python:3.11-alpine
LABEL maintainer="<Nom> <Prénom>"

# Create a specific user to run the Python application
RUN adduser -D my-user -u 1000

# Create and change working directory
WORKDIR /app

# Add application requirements
COPY requirements.txt .

# Install requirements
RUN pip install -r requirements.txt

# Change use post-install to have all dependancies available
USER 1000

# Add application
COPY app.py .
COPY config.py .

# Launch application
ENTRYPOINT ["gunicorn"]
CMD ["--config", "config.py", "app:app"]
```

Prenons le temps d'étudier ces commandes :

`FROM` est une directive permettant de qualifier l'image de base utilisée. Pas de mystère, on utilise une version d’AlpineLinux.

`LABEL` est une directive qui permet d’ajouter des metadata à l’image. Ici, cela nous permet de définir la personne maintenant l'image. **Remplacez par vos nom et prénoms**.

`RUN` est l'une des directives principales d'un Dockerfile et permet d'exécuter des instructions qui vont permettre de construire notre image. Ici, on installe les dépendances de notre application avec pip.

`COPY` est une directive qui permet d’ajouter des fichiers provenant de notre host dans une image. Une option au comportement similaire est ADD, mais elle n’est pas conseillée vu son caractère “automagique”. Elle décompresse des fichiers et marche avec n’importe quel type de ressource, ce qui rend difficile de prévoir son comportement...

`WORKDIR` est une directive qui permet de changer le working directory par défaut de notre image.

`USER` est une directive permettant de spécifier quel sera l’utilisateur courant. Il est important de spécifier un utilisateur non-root (avec un uid supérieur ou égal à 1000) pour des raisons de sécurité.

`ENTRYPOINT` et `CMD` permettent de définir la commande qui est lancée au démarrage de notre conteneur. On parlera des différences entre ces deux directives plus tard dans la formation.

Maintenant que nous avons créé notre fichier Dockerfile, il est temps de créer notre fichier docker-compose qui va nous simplifier la construction et le lancement de notre application. Nous allons créer un fichier `docker-compose.yml` (toujours dans  `/workspace/app/`) avec le contenu suivant :
```yaml
# docker-compose.yml
---
services:
  app:
    build: .
    image: "<username>/app:v0.1"
    ports:
    - "8000:8000"
```

Nous allons à présent utiliser Docker Compose une première fois pour construire notre image :
```sh
docker compose build
```
```text
[+] Building 11.9s (12/12) FINISHED
 => [app internal] load build definition from Dockerfile
 => => transferring dockerfile: 558B
 => [app internal] load metadata for docker.io/library/python:3.11-alpine
 => [app internal] load .dockerignore
 => => transferring context: 2B
 => [app 1/7] FROM docker.io/library/python:3.11-alpine@sha256:f3d31c8677d03f0b3c724446077f229a6ce9d3ac430f5c08cd7dff00292048c3
 => => resolve docker.io/library/python:3.11-alpine@sha256:f3d31c8677d03f0b3c724446077f229a6ce9d3ac430f5c08cd7dff00292048c3
 => => sha256:148762f75a1f92cc9857e9c488bf95d5aac61e9905ec47a7408025b2dd5c3b7a 240B / 240B
 => => sha256:f3d31c8677d03f0b3c724446077f229a6ce9d3ac430f5c08cd7dff00292048c3 1.65kB / 1.65kB
 => => sha256:e6da3ee9bb64dd12b98fa609487f112fe1e365522e6e8345309db15c22a80a51 1.37kB / 1.37kB
 => => sha256:1bac8ae77e4af0b868b62a75115616a20e025e0451eeed05d94a4cfc4523e58a 6.87kB / 6.87kB
 => => sha256:9875af95546db78168a6761b7fa205ed1cd0c153cd89356c1512e551c12b2d5c 622.29kB / 622.29kB
 => => sha256:4819c95424fc4a94767c9329b02238ebcce0bc682384cb671379bc1fb8a12b55 10.94MB / 10.94MB
 => => extracting sha256:9875af95546db78168a6761b7fa205ed1cd0c153cd89356c1512e551c12b2d5c
 => => sha256:ea1518237b3753b3fe40ee773d77651704178d9baa72ae5012e13a992cfa6c63 2.85MB / 2.85MB
 => => extracting sha256:4819c95424fc4a94767c9329b02238ebcce0bc682384cb671379bc1fb8a12b55
 => => extracting sha256:148762f75a1f92cc9857e9c488bf95d5aac61e9905ec47a7408025b2dd5c3b7a
 => => extracting sha256:ea1518237b3753b3fe40ee773d77651704178d9baa72ae5012e13a992cfa6c63
 => [app internal] load build context
 => => transferring context: 936B
 => [app 2/7] WORKDIR /app
 => [app 3/7] COPY requirements.txt .
 => [app 4/7] RUN pip install -r requirements.txt
 => [app 5/7] COPY app.py .
 => [app 6/7] COPY config.py .
 => [app 7/7] RUN adduser -D my-user -u 1000
 => [app] exporting to image
 => => exporting layers
 => => writing image sha256:b9bfc885e157598a5b8ac0da27175435182fb64af66d5b4fddcf892e8dc888e7
 => => naming to william/app:v0.1
```
Cette commande utilise `docker compose`, mais en réalité, elle ne fait qu’encapsulser pour vous un appel à la commande `docker image build` :
```sh
docker image build -t=<username>/app:v0.1 .
```
Cette commande ordonne à Docker de créer une nouvelle image en respectant les instructions contenues dans le Dockerfile que nous venons de créer. Nous affectons également un nom à notre nouvelle image afin de pouvoir l'identifier avec le flag -t. Nous pouvons optionnellement affecter un **tag** de version différent de `latest` en ajoutant “:” puis le nom de la version à la suite du nom de l’image (`v0.1` dans notre cas).

L'exécution des instructions avec la commande `RUN` suit un workflow très particulier :
1. Docker lance un conteneur à partir de l'image de base
2. Une instruction est exécutée, par exemple `pip install -r requirements.txt` et un changement est effectué dans le conteneur.
3. Docker déclenche l'équivalent d'un `docker container commit` afin de créer une nouvelle couche de fichiers. Puis il crée une nouvelle image à partir de cette couche par dessus les couches précédentes.
4. Docker lance un nouveau conteneur grâce à cette nouvelle image.
5. On répète jusqu'à ce que toutes les commandes `RUN` soient exécutées et que l'on soit en possession d'une image finale à utiliser.


On peut remonter dans les actions effectuées pour la construction de notre image avec la commande `docker image history`.
```
dev $ sudo docker image history 31920ffe5099
IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
31920ffe5099        4 minutes ago       /bin/sh -c #(nop)  CMD ["--config" "config.p…   0B
39c5abb4193d        4 minutes ago       /bin/sh -c #(nop)  ENTRYPOINT ["gunicorn"]      0B
06b4274f9bae        4 minutes ago       /bin/sh -c #(nop)  USER 1000                    0B
f4c50ec0ccb9        4 minutes ago       /bin/sh -c adduser -D my-user -u 1000           4.82kB
dc54fc18a0a7        4 minutes ago       /bin/sh -c #(nop) COPY file:3e50f9b4a355f9de…   138B
597e9dfdca20        4 minutes ago       /bin/sh -c #(nop) COPY file:f2123582ae0ff603…   955B
f1dac0f790ee        4 minutes ago       /bin/sh -c pip install -r requirements.txt      11.3MB
fb52dbeb058f        4 minutes ago       /bin/sh -c #(nop) COPY file:70c56863bd832380…   63B
d07cfb69c642        4 minutes ago       /bin/sh -c #(nop) WORKDIR /app                  0B
a2e9b278045e        28 minutes ago      /bin/sh -c #(nop)  LABEL maintainer=<Nom> <P…   0B
c02a3409ee5b        6 hours ago         /bin/sh -c #(nop)  CMD ["python3"]              0B
<missing>           6 hours ago         /bin/sh -c set -ex;   wget -O get-pip.py 'ht…   6.04MB
<missing>           6 hours ago         /bin/sh -c #(nop)  ENV PYTHON_PIP_VERSION=19…   0B
<missing>           2 weeks ago         /bin/sh -c cd /usr/local/bin  && ln -s idle3…   32B
<missing>           2 weeks ago         /bin/sh -c set -ex  && apk add --no-cache --…   74.6MB
<missing>           2 weeks ago         /bin/sh -c #(nop)  ENV PYTHON_VERSION=3.7.2     0B
<missing>           2 weeks ago         /bin/sh -c #(nop)  ENV GPG_KEY=0D96DF4D4110E…   0B
<missing>           2 weeks ago         /bin/sh -c apk add --no-cache ca-certificates   551kB
<missing>           2 weeks ago         /bin/sh -c #(nop)  ENV LANG=C.UTF-8             0B
<missing>           3 weeks ago         /bin/sh -c #(nop)  ENV PATH=/usr/local/bin:/…   0B
<missing>           3 weeks ago         /bin/sh -c #(nop)  CMD ["/bin/sh"]              0B
<missing>           3 weeks ago         /bin/sh -c #(nop) ADD file:2a1fc9351afe35698…   5.53MB
```

Nous pouvons maintenant voir la totalité des commandes lancées lors de l'exécution de la
commande `docker image build`. Les instructions exécutées sont triées de la plus récente à la plus ancienne. Seule les 8 premières nous intéressent car ce sont les commandes produites par notre Dockerfile (les autres concernent les couches inférieures de l'image alpine). Chacune des instructions du Dockerfile produit bien une couche de fichiers puis une image avec un ID que Docker réutilise pour construire la couche suivante.

Il est intéressant de noter qu'il existe une limitation importante à Docker ([ou plutôt du système de fichiers dit union fs : Overlay2](https://github.com/docker/docker.github.io/issues/8230#issuecomment-468630187)) : **Nous sommes limités à 127 couches de fichiers !**

Si nous créons un Dockerfile et que nous atteignons la limite de 127 couches, nous ne pouvons plus construire par-dessus. Bien qu'en pratique cette limite est difficilement atteignable, il est important de l'avoir en tête afin de factoriser au maximum les instructions dans notre Dockerfile.

Un avantage de la factorisation à lieu lors de la suppression des fichiers. En effet, comme dans Git, la suppression d’un fichier lors d’un commit ne le supprime pas de l’historique (la couche précédente dans notre cas). Il est donc recommandé de procéder aux tâches de purge dans le mêmes lignes que celle qui font des installations. Exemple :
```Dockerfile
RUN apt-get -y update && apt-get install -y \
  aufs-tools \
  automake \
  build-essential \
  curl \
  dpkg-sig \
  libcap-dev \
  libsqlite3-dev \
  mercurial \
  reprepo \
  ruby1.9.1 \
  ruby1.9.1-dev \
  s3cmd=1.1.* \
  && rm -rf /var/lib/apt/lists/*
```
Notez bien que toutes les commandes sont enchaînées dans une seule instruction `RUN` du `Dockerfile` via des `&&` et se termine par un `rm`. Une seule couche est créée et les fichiers temporaires sont supprimés avant le commit.

Il est à présent temps de démarrez l’application. Pour ce faire, exécutez la commande suivante :
```sh
docker compose up
```
```text
[+] Running 2/2
 ✔ Network app_default  Created                                            0.5s 
 ✔ Container app-app-1  Created                                            1.5s 
Attaching to app-1
app-1  | [2024-10-06 14:21:10 +0000] [1] [INFO] Starting gunicorn 22.0.0
app-1  | [2024-10-06 14:21:10 +0000] [1] [INFO] Listening at: http://0.0.0.0:8000 (1)
app-1  | [2024-10-06 14:21:10 +0000] [1] [INFO] Using worker: gthread
app-1  | [2024-10-06 14:21:10 +0000] [6] [INFO] Booting worker with pid: 6

```

Ouvrez une autre session shell et essayez de requêter l’application :
```sh
curl localhost:8000 -v
```
```text
*   Trying 127.0.0.1:8000...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8000 (#0)
> GET / HTTP/1.1
> Host: localhost:8000
> User-Agent: curl/7.68.0
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Server: gunicorn
< Date: Sun, 06 Oct 2024 14:21:35 GMT
< Connection: keep-alive
< Content-Type: text/html; charset=utf-8
< Content-Length: 14
< 
Hello Docker!
* Connection #0 to host localhost left intact

```
On voit que l’application répond avec un status code HTTP 200 et le message “Hello Docker!”.

## 9- Transférer notre image sur le registre

Maintenant que nous avons notre image locale, le but est de la transférer sur le **registre central** afin que tout le monde puisse en profiter en appelant la commande `docker image pull`. Transférons notre image **app** sur le **registre** que nous avons utilisé plus tôt lors de ce TP. Pour effectuer cette action, nous devons utiliser la commande `docker compose push`.
```sh
docker compose push
```
```text
Pushing app (<username>/app:v0.1)...
The push refers to a repository [<username>/app]
fe248af17b1b: Pushed
5a54a6dcff20: Pushed
50f589ab1c5f: Pushed
75fed4290ceb: Pushed
6795dbd93463: Pushed
e2986b5e7ba2: Pushed
beefb6beb20f: Pushed
df64d3292fd6: Pushed
v0.1: digest: sha256:683e3fa0b802b4216c80434ae688124b48cec58bfba628bd56944c9e5a614c78 size: 1993
```
Cette commande utilise `docker compose`, mais en réalité, elle ne fait qu’encapsulser pour vous un appel à la commande `docker image push` :
```sh
docker image push <username>/app:v0.1
```
En analysant la trace, on remarque que Docker détecte que nous voulons pousser notre image dans le dépôt **app** situé dans le registre accessible à l'adresse `<username>/app`.
Ensuite, Docker transfère une à une les couches qui forment notre image sur notre registre. Notre registre contient ainsi notre image, et s'il était public et accessible, n'importe qui pourrait appeler la commande `docker image pull` afin de télécharger l'image et ensuite appeler `docker container run` pour construire un conteneur à partir de cette image.

## 10- Nettoyage des conteneurs

Nous allons supprimer tous les conteneurs :
```sh
docker system prune
```
La commande `prune` nous permet de supprimer les ressources Docker qui ne sont plus utilisées dans notre machine. Dans ces ressources, on trouve les conteneurs arrêtés, les réseaux non-utilisés, les images sans nom (dangling), et les volumes.

## 11- Résumé des commandes

Bravo! Vous avez pris en main les commandes de base de Docker afin de créer et manipuler des images. Dans la prochaine section, nous allons voir quelques commandes supplémentaires pour créer un Dockerfile. En attendant voici un petit résumé des commandes que nous avons vues dans cette section:

`docker image ls` Liste les images que l'on possède en local.
`docker search` Cherche une image dans le registre central (Docker Hub)
`docker image pull` Télécharge une image depuis le Docker Hub ou un registre privé.

`docker container commit` Sauvegarde les changements réalisés dans une image (installation d'un paquet, etc.) et crée une image réutilisable.

`Dockerfile` Fichier permettant de créer une image en décrivant une suite d'instructions.
- `FROM` L'image de base que l'on doit utiliser pour la construction de notre nouvelle image.
- `MAINTAINER` Le créateur/mainteneur de l'image.
- `RUN` Exécute une commande shell et crée une nouvelle couche de système de fichiers en fonctions des changements effectués.

`docker image build` Construit une image à partir d'un Dockerfile.
- `--tag / -t` Nom de l'image à construire.

`docker image history` Donne des informations sur la hiérarchie de couches de 	notre conteneur.

`docker image push` Transfère l'image sur le registre central afin de la partager 	avec la communauté ou d'autres développeurs.
---
layout: page
permalink: /docs/resources.html
---

# Resources API

La API de recursos en corbel está implementada sobre una interfaz muy flexible que permite servir cualquier tipo de representación de un recurso. usando los patrones definidos por esta API, se puede desplegar cualquier tipo de recurso con un impacto mínimo sobre los clientes y servidor.

## Versionado

El primer nivel de todas las URL de la API es siempre la versión de ésta última, y tiene la forma v{número}. 

```
https://resources-staging.bqws.io/v1.0/media:Track/555
```

## URIs: completas vs forma canónica

Todo recurso en la API se identifica a través de su URI. En corbel, por razones prácticas, siempre utilizamos la forma canónoca de la URI (canonical form), formada por namespace:id, en lugar de la URI 'completa' (fully quilified name). Los namespaces se definen mediante la propia API y los clientes los pueden consultar en {resources_api}/namespaces


### Ejemplo namespaces

```
{
    "media": "http://ontology.bqreaders.com/media/",
    "product": "http://ontology.bqreaders.com/product/",
    "entity: "http://ontology.bqreaders.com/entity/",
    "api": "http://ontology.bqreaders.com/api/"
}
```
Con estos namespaces podíramos crear las siguientes formas canónicas

|URI|Canonical form|
|---|---|
|http://ontology.bqreaders.com/media/Album|media:Album|
|http://ontology.bqreaders.com/product/Cellphone/aquaris5|product:Cellphone/aquaris5|
|http://ontology.bqreaders.com/entity/Publisher/planeta|entity:Publisher/planeta|
|http://ontology.bqreaders.com/api/limit|api:limit|

----------

# URI templates

corbel cuenta únicamente con 3 formas diferentes de URL

* **Collection**: conjunto de recursos. URI template: resources_api/{collection}

```
resources_api/media:Track
resources_api/product:Cellphone
resources_api/entity:Artist
```

* **Resource**: recurso particular. URI template: resources_api/{collection}/{resource_id}

```
resources_api/media:Track/555
resources_api/product:Cellphone/aquaris5
resources_api/entity:Artist/123
```

* **Relation**: relación entre un recurso particular y una colección. URI template: resources_api/{collection}/{resource_id}/{relation_collection}

```
resources_api/media:Album/456/media:tracks
resources_api/media:Book/555/media:authors
resources_api/media:Videogame/asdf/media:related
```

----------

# Parámetros de la petición

La petición puede contener parámetros que modifican la representación de la respuesta. Dichos parámetros deben ser especificados mediante su forma conónica.

## Parámetros permitidos

|Option|Description|Possible Values|Example|
|---|---|---|---|
|api:pageSize|Limits the number of resources returned from a collection in one page|Positive Integer|/resource/entity:Artist/456/albums?api:pageSize=5|
|api:page|The page of the results returned from a collection|Positive Integer|/resource/entity:Artist/456/albums?api:page=5&api:pageSize=5|
|api:sort|Sorts the resources returned in the direction given|JSON String|/resource/media:Artist?api:sort={"meta:releaseDate":"desc"}|
|api:query|Query for a collection of resources. Using the query language described by this document|JSON Query String|/resource/media:Artist?api:query={"meta:label":"The Killers"}|
|api:aggregation|Do aggreagtion operations with the resources|JSON Aggregation String|/resource/media:Artist?api:aggregation={"$count":"*"}|

----------

# API query language
# API aggregation language
# Negociación de contenido
# Implementación

![resources implementation](/img/resources-implementation.png)

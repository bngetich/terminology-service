# Terminology Service (SNOMED • RxNorm • LOINC)

A lightweight terminology resolution service that converts free‑text
clinical terms into standardized codes using:

-   SNOMED CT (problems/conditions)
-   RxNorm (medications)
-   LOINC (labs & observations)

Built with Spring Boot + Elasticsearch --- deterministic, lexical, and
honest.

## Features

-   Deterministic term normalization
-   Exact lookup via Elasticsearch
-   Multiple terminologies in one index
-   Idempotent imports (safe re-run)
-   Resolve API returns:
    -   matched
    -   ambiguous
    -   no_match

## Architecture

Client → /terminology/resolve → Terminology Service → Elasticsearch

## Run Elasticsearch

docker run -d -p 9200:9200 -e "discovery.type=single-node"
docker.elastic.co/elasticsearch/elasticsearch:8.13.0

## Run Service

mvn spring-boot:run

## Resolve Example

POST /terminology/resolve { "text": "type 2 diabetes", "system":
"http://snomed.info/sct" }

## Licensing

Terminology data must be obtained from official sources (SNOMED, RxNorm,
LOINC).

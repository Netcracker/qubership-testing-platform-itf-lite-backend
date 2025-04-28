# Qubership ITF Lite Installation Guide

### 3rd party dependencies

| Name       | Version | Mandatory/Optional | Comment                |
|------------|---------|--------------------|------------------------|
| PostgreSQL | 14+     | Mandatory          | JDBC connection string |
| GridFS     | 4.2+    | Mandatory          | For storing files      |

### HWE

|                  | CPU request | CPU limit | RAM request | RAM limit |
|------------------|-------------|-----------|-------------|-----------|
| Dev level        | 50m         | 500m      | 300Mi       | 1500Mi    |
| Production level | 50m         | 1500m     | 3Gi         | 3Gi       |

### Minimal parameters set

```properties
-DKEYCLOAK_AUTH_URL=
-DKEYCLOAK_ENABLED=
-DKEYCLOAK_REALM=
-DKEYCLOAK_CLIENT_NAME=
-DKEYCLOAK_SECRET=
-DEUREKA_CLIENT_ENABLED=true
-DSERVICE_REGISTRY_URL=
-DPG_DB_ADDR=
-DPG_DB_PORT=
-DATP_ITF_LITE_DB=
-DATP_ITF_LITE_DB_USER=
-DATP_ITF_LITE_DB_PASSWORD=
-DGRID_DBNAME=
-DGRIDFS_DB_ADDR=
-DGRIDFS_DB_PORT=
-DATP_ITF_LITE_GRIDFS_DB_USER=
-DATP_ITF_LITE_GRIDFS_DB_PASSWORD=
-DKAFKA_SERVERS=
-DATP_CRYPTO_KEY=
-DATP_CRYPTO_PRIVATE_KEY=
```

**NOTE:** database will be pre-created by Liquidbase prescripts

### Full ENV VARs list per container

| Deploy Parameter Name                          | Mandatory | Example                                                                          | Description                                          |
|------------------------------------------------|-----------|----------------------------------------------------------------------------------|------------------------------------------------------|
| `ADAPTER_TYPE`                                 | No        | kafka                                                                            | Type of logging adapter                              |
| `ATP_CATALOGUE_URL`                            | No        | https://atp-catalogue-devci.dev-atp-cloud.com                                    | Catalog service URL                                  |
| `ATP_HTTP_LOGGING`                             | No        | true                                                                             | Enable or Disable logging controls                   |
| `ATP_HTTP_LOGGING_HEADERS`                     | No        | true                                                                             | Enable or Disable request headers logging            |
| `ATP_HTTP_LOGGING_HEADERS_IGNORE`              | No        | Authorization                                                                    | Headers that will be ignored during logging          |
| `ATP_HTTP_LOGGING_URI_IGNORE`                  | No        | /atp-itf-lite/api/v1/sse/.* /rest/deployment/readiness /rest/deployment/liveness | Endpoints that will be ignored during logging        |
| `ATP_INTERNAL_GATEWAY_ENABLED`                 | No        | true                                                                             | Enable or Disable internal gateway routing           |
| `ATP_ITF_LITE_CLOSE_IDLE_CONNECTION_WAIT_TIME` | Yes       | 60                                                                               | Timeout or delay setting in milliseconds             |
| `ATP_ITF_LITE_CONNECTION_TIMEOUT`              | Yes       | 30000                                                                            | Timeout or delay setting in milliseconds             |
| `ATP_ITF_LITE_DB`                              | Yes       | dev04_itf_lite                                                                   | Database name                                        |
| `ATP_ITF_LITE_DEFAULT_KEEP_ALIVE_TIME_MILLIS`  | Yes       | 20000                                                                            | Keep-alive timeout value                             |
| `ATP_ITF_LITE_HTTP_REQUEST_SIZE_MB`            | Yes       | 100                                                                              | Request size limit                                   |
| `ATP_ITF_LITE_HTTP_RESPONSE_SIZE_MB`           | Yes       | 100                                                                              | Response size limit                                  |
| `ATP_ITF_LITE_MAX_TOTAL_CONNECTIONS`           | Yes       | 100                                                                              | Max total http connections                           |
| `ATP_ITF_LITE_PING_SSE_TIMEOUT`                | Yes       | 30000                                                                            | SSE ping timeout or delay setting in milliseconds    |
| `ATP_ITF_LITE_REQUEST_TIMEOUT`                 | Yes       | 30000                                                                            | Request timeout or delay setting in milliseconds     |
| `ATP_ITF_LITE_SOCKET_TIMEOUT`                  | Yes       | 480000                                                                           | Socket timeout or delay setting in milliseconds      |
| `ATP_ITF_LITE_SSE_TIMEOUT`                     | Yes       | 60000                                                                            | SSE timeout or delay setting in milliseconds         |
| `ATP_NOTIFICATION_MODE`                        | No        | kafka                                                                            | Type of notification mode                            |
| `AUDIT_LOGGING_ENABLE`                         | No        | false                                                                            | Enable or Disable audit logging                      |
| `AUDIT_LOGGING_TOPIC_NAME`                     | No        | dev04_audit_logging_topic                                                        | Audit logging Kafka topic name                       |
| `AUDIT_LOGGING_TOPIC_PARTITIONS`               | No        | 1                                                                                | Audit logging Kafka topic partitions number         |
| `AUDIT_LOGGING_TOPIC_REPLICAS`                 | No        | 3                                                                                | Audit logging Kafka replicas number                  |
| `CLEANUP_COLLECTION_RUNS_CRON_EXPRESSION`      | No        | 0 0 0 * * ?                                                                      | CRON expression for collection runs cleanup job      |
| `CLEANUP_COOKIES_CRON_EXPRESSION`              | No        | 0 0 * * * *                                                                      | CRON expression for cookies cleanup job              |
| `CLEANUP_SNAPSHOTS_CRON_EXPRESSION`            | No        | 0 0 0 * * ?                                                                      | CRON expression for snapshots cleanup job            |
| `CLEANUP_SNAPSHOTS_EXPIRATION_PERIOD_SECONDS`  | No        | 86400                                                                            | Cleanup job configuration                            |
| `COLLECTION_RUNS_REMOVE_DAYS`                  | No        | 1                                                                                | Cleanup period in days for collection runs           |
| `CONNECTIONS_PER_HOST`                         | No        | 100                                                                              | Limit of connection per host                         |
| `CONSUL_ENABLED`                               | No        | false                                                                            | Enable or disable Consul                             |
| `CONSUL_HEALTH_CHECK_ENABLED`                  | No        | false                                                                            | Enable or disable Consul healthcheck                 |
| `CONSUL_PORT`                                  | No        | 8500                                                                             | Consul port number                                   |
| `CONSUL_PREFIX`                                | No        | devci                                                                            | Consul prefix value                                  |
| `CONTENT_SECURITY_POLICY`                      | No        | default-src 'self                                                                | Security policy settings for frontend                |
| `EI_CLEAN_JOB_ENABLED`                         | No        | true                                                                             | Enable or disable export and import job              |
| `EI_CLEAN_JOB_FILE_DELETE_AFTER_MS`            | No        | 172800000                                                                        | Export and import file delete period in milliseconds |
| `EI_CLEAN_JOB_WORKDIR`                         | No        | exportimport/node                                                                | Export and import cleanup job work directory         |
| `EI_CLEAN_SCHEDULED_JOB_PERIOD_MS`             | No        | 86400000                                                                         | Export and import cleanup job period in milliseconds |
| `EI_GRIDFS_DB`                                 | No        | dev04_ei_gridfs                                                                  | Export and import database name                      |
| `EI_GRIDFS_DB_ADDR`                            | No        | mongos.mongocluster.svc                                                          | Export and import database host address              |
| `EI_GRIDFS_DB_PORT`                            | No        | 27017                                                                            | Export and import database port number               |
| `EI_SERVICE_URL`                               | No        | http://atp-export-import-dev04.atp2k8.managed.cloud                              | Export and import service URL                        |
| `EUREKA_CLIENT_ENABLED`                        | No        | true                                                                             | Enable or disable Eureka                             |
| `FEIGN_ATP_CATALOGUE_NAME`                     | No        | ATP-CATALOGUE                                                                    | Feign Catalog client name                            |
| `FEIGN_ATP_CATALOGUE_ROUTE`                    | No        | api/atp-catalogue/v1                                                             | Feign Catalog client name route                      |
| `FEIGN_ATP_EI_NAME`                            | No        | ATP-EXPORT-IMPORT                                                                | Feign Export and import client name                  |
| `FEIGN_ATP_EI_ROUTE`                           | No        | api/atp-export-import/v1                                                         | Feign Export and import client route                 |
| `FEIGN_ATP_ENVIRONMENTS_NAME`                  | No        | ATP-ENVIRONMENTS                                                                 | Feign Environments client name                       |
| `FEIGN_ATP_ENVIRONMENTS_ROUTE`                 | No        | api/atp-environments/v1                                                          | Feign Environments client route                      |
| `FEIGN_ATP_INTERNAL_GATEWAY_NAME`              | No        | atp-internal-gateway                                                             | Feign Internal gateway client name                   |
| `FEIGN_ATP_ITF_ENABLED`                        | No        | true                                                                             | Enable or disable ITF integration                    |
| `FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_NAME`        | Yes       | ATP-IFT-LITE-SCRIPT-ENGINE                                                       | Feign Script Engine client name                      |
| `FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_URL`         | Yes       | http://atp-itf-lite-script-engine:8080                                           | Feign Script Engine client URL                       |
| `FEIGN_ATP_ITF_NAME`                           | No        | ATP-ITF-EXECUTOR                                                                 | Feign ITF client name                                |
| `FEIGN_ATP_ITF_ROUTE`                          | No        | api/atp-itf-executor/v1                                                          | Feign ITF client route                               |
| `FEIGN_ATP_MACROS_NAME`                        | No        | ATP-MACROS                                                                       | Feign Macros client name                             |
| `FEIGN_ATP_MACROS_ROUTE`                       | No        | api/atp-macros/v1                                                                | Feign Macros client route                            |
| `FEIGN_ATP_NOTIFICATION_NAME`                  | No        | ATP-NOTIFICATION                                                                 | Feign Notification client name                       |
| `FEIGN_ATP_NOTIFICATION_ROUTE`                 | No        | api/atp-notification/v1                                                          | Feign Notification client route                      |
| `FEIGN_ATP_RAM_NAME`                           | No        | ATP-RAM                                                                          | Feign RAM client name                                |
| `FEIGN_ATP_RAM_ROUTE`                          | No        | api/atp-ram/v1                                                                   | Feign RAM client route                               |
| `FEIGN_ATP_USERS_NAME`                         | No        | ATP-USERS-BACKEND                                                                | Feign Users client name                              |
| `FEIGN_ATP_USERS_ROUTE`                        | No        | api/atp-users-backend/v1                                                         | Feign Users client route                             |
| `GET_ACCESS_TOKEN_RETENTION_CRON_EXPRESSION`   | No        | 0 30 2 ? * *                                                                     | CRON expression for access token retention           |
| `GRAYLOG_HOST`                                 | No        | tcp:graylog01cn.com                                                              | Graylog log host address                             |
| `GRAYLOG_ON`                                   | No        | true                                                                             | Enable or disable Graylog integration                |
| `GRAYLOG_PORT`                                 | No        | 12204                                                                            | Graylog port value                                   |
| `GRIDFS_DB_ADDR`                               | No        | mongos.mongocluster.svc                                                          | GridFS host address                                  |
| `GRIDFS_DB_PORT`                               | No        | 27017                                                                            | GridFS port number                                   |
| `GRID_CHUNK_SIZE`                              | No        | 1024                                                                             | GridFS chunk size                                    |
| `GRID_DBNAME`                                  | No        | dev04_itf_lite_gridfs                                                            | GridFS database name                                 |
| `GRID_DICTIONARY_REMOVE_DAYS`                  | No        | 30                                                                               | Cleanup period in days for dictionary                |
| `HAZELCAST_ADDRESS`                            | No        | atp-hazelcast.dev04.svc:5701                                                     | Hazelcast host address                               |
| `HAZELCAST_CLIENT_ENABLED`                     | No        | false                                                                            | Enable or disable Hazelcast client integration       |
| `HAZELCAST_CLUSTER_NAME`                       | No        | atp-hc                                                                           | Hazelcast cluster name                               |
| `HAZELCAST_SERVER_ENABLED`                     | No        | false                                                                            | Enable or disable Hazelcast server integration       |
| `HISTORY_CLEAN_JOB_EXPRESSION`                 | No        | 0 10 0 * * ?                                                                     | Cleanup job expression fo History                    |
| `HISTORY_CLEAN_JOB_PAGE_SIZE`                  | No        | 100                                                                              | History cleanup job page size                        |
| `HISTORY_CLEAN_JOB_REVISION_MAX_COUNT`         | No        | 100                                                                              | History cleanup job revision max count               |
| `HISTORY_RETENTION_CRON_EXPRESSION`            | No        | 0 0 2 ? * *                                                                      | History retention CRON expression                    |
| `HISTORY_RETENTION_IN_DAYS`                    | No        | 14                                                                               | History retention period in days                     |
| `ITF_LITE_CLEAN_CACHED_FILES`                  | No        | 3600                                                                             | Cleanup job period for cached files in seconds       |
| `JAVA_OPTIONS`                                 | No        | -Dcom.sun.management.jmxremote=true ...                                          | Java command line options                            |
| `JAVERS_ENABLED`                               | No        | false                                                                            | Enable or disable Javers integration                 |
| `KAFKA_CATALOG_GROUP`                          | No        | dev04_catalog_notification_group                                                 | Kafka catalog group name                             |
| `KAFKA_CATALOG_TOPIC`                          | No        | dev04_catalog_notification_topic                                                 | Kafka catalog topic name                             |
| `KAFKA_ENVIRONMENT_TOPIC`                      | No        | dev04_environments_notification_topic                                            | Kafka environment topic name                         |
| `KAFKA_ITF_LITE_EXECUTION_FINISH_PARTITIONS`   | No        | 1                                                                                | Kafka execution finish partitions number             |
| `KAFKA_ITF_LITE_EXECUTION_FINISH_REPLICAS`     | No        | 3                                                                                | Kafka execution finish replicas number               |
| `KAFKA_ITF_LITE_EXECUTION_FINISH_TOPIC`        | No        | dev04_itf_lite_execution_finish                                                  | Kafka execution finish topic name                    
| `KAFKA_LOGRECORD_TOPIC`                        | No        | dev04_orch_logrecord_topic                                                       | Kafka log record topic name                          |
| `KEYCLOAK_AUTH_URL`                            | Yes       | https://atp-keycloak-dev04.atp2k8.managed.cloud/auth                             | Keycloak auth URL                                    |
| `KEYCLOAK_ENABLED`                             | Yes       | true                                                                             | Enable or disable Keycloak integration               |
| `KEYCLOAK_REALM`                               | Yes       | atp2                                                                             | Keycloak realm name                                  |
| `LOG_LEVEL`                                    | No        | INFO                                                                             | Service logging level                                |
| `MAX_CONNECTION_IDLE_TIME`                     | No        | 0                                                                                | Service max connection idle time                     |
| `MAX_RAM`                                      | No        | 1024m                                                                            | Memory usage limit                                   |
| `MICROSERVICE_NAME`                            | No        | atp-itf-lite-backend                                                             | Service system name                                  |
| `MIN_CONNECTIONS_PER_HOST`                     | No        | 40                                                                               | Minimum connections per host name                    |
| `PG_DB_ADDR`                                   | Yes       | pg-patroni.postgrescluster.svc                                                   | Postgres database host address                       |
| `PG_DB_PORT`                                   | Yes       | 5432                                                                             | Postgres database port number                        |
| `PROFILER_ENABLED`                             | No        | false                                                                            | Enable or disable profiler                           |
| `PROJECT_INFO_ENDPOINT`                        | No        | /api/v1/users/projects                                                           | Project metadata API endpoint                        |
| `REMOTE_DUMP_HOST`                             | No        | profiler-collector-service.profiler.svc                                          | Remote dump service host address                     |
| `REMOTE_DUMP_PORT`                             | No        | 1710                                                                             | Remote dump service port value                       |
| `SERVICE_ENTITIES_MIGRATION_ENABLED`           | No        | true                                                                             | Enable or disable entities migration                 |
| `SERVICE_NAME`                                 | No        | atp-itf-lite-backend                                                             | Service system name                                  |
| `SERVICE_REGISTRY_URL`                         | No        | http://atp-registry-service.dev04.svc:8761/eureka                                | Service registry endpoint URL                        |
| `SPRING_PROFILES`                              | No        | default                                                                          | Spring active profiles                               |
| `SWAGGER_ENABLED`                              | No        | false                                                                            | Enable or disable Swagger integration                |
| `ZIPKIN_ENABLE`                                | No        | true                                                                             | Enable or disable Zipkin distributed tracing         |
| `ZIPKIN_PROBABILITY`                           | No        | 1.0                                                                              | Zipkin probability level                             |
| `ZIPKIN_URL`                                   | No        | http://jaeger-app-collector.jaeger.svc:9411                                      | Zipkin host address                                  |                    |

## Helm

### Prerequisites

1. Install k8s locally
2. Install Helm

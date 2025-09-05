# How to start backend locally

## 1. Clone repository `git clone <itf lite repo url>`

## 2. Build the project `mvn -P github clean install`

## 3. Create new configuration

    1) Go to Run menu and click Edit Configuration
    2) Add new Application configuration
    3) Set parameters:
    4) Add the following parameters in VM options - click Modify Options and select "Add VM Options":
```properties
-Dserver.port=
-DMONITOR_PORT=

-DSPRING_PROFILES=
-DKEYCLOAK_AUTH_URL=
-DKEYCLOAK_ENABLED=
-DKEYCLOAK_REALM=
-DKEYCLOAK_CLIENT_NAME=
-DKEYCLOAK_SECRET=

-DEUREKA_CLIENT_ENABLED=
-DSERVICE_REGISTRY_URL=

-Dspring.output.ansi.enabled=ALWAYS
-Dlogging.level.org.qubership.atp.common.logging.interceptor.RestTemplateLogInterceptor=info
-Dlogging.level.org.qubership.atp.catalogue.service.client.feign.DatasetFeignClient=info
-DATP_HTTP_LOGGING=

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

-DFEIGN_ATP_CATALOGUE_URL=
-DFEIGN_ATP_MACROS_URL=
-DFEIGN_ATP_USERS_URL=
-DFEIGN_ATP_ITF_URL=
-DFEIGN_ATP_ENVIRONMENTS_URL=
-DFEIGN_ATP_RAM_URL=
-DFEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_URL=

-DATP_CRYPTO_KEY=
-DATP_CRYPTO_PRIVATE_KEY=
```

## 4. Click `Apply` and `OK`

## 5. Run the project

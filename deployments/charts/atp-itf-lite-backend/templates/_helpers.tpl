{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
{{ printf "- name: %s" $key }}
{{ printf "  value: \"%s\"" $val }}
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- if eq (default "" .Values.ENCRYPT) "secrets" }}
{{- $keys = concat $keys (list "ATP_CRYPTO_KEY" "ATP_CRYPTO_PRIVATE_KEY") }}
{{- end }}
{{- range $keys }}
{{ printf "- name: %s" . }}
{{ printf "  valueFrom:" }}
{{ printf "    secretKeyRef:" }}
{{ printf "      name: %s-secrets" $.Values.SERVICE_NAME }}
{{ printf "      key: %s" . }}
{{- end }}
{{- end }}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ADAPTER_TYPE: "{{ .Values.ADAPTER_TYPE }}"
ATP_CATALOGUE_URL: "{{ .Values.ATP_CATALOGUE_URL }}"
ATP_HTTP_LOGGING: "{{ .Values.ATP_HTTP_LOGGING }}"
ATP_HTTP_LOGGING_HEADERS: "{{ .Values.ATP_HTTP_LOGGING_HEADERS }}"
ATP_HTTP_LOGGING_HEADERS_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_HEADERS_IGNORE }}"
ATP_HTTP_LOGGING_URI_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_URI_IGNORE }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_ITF_LITE_CLOSE_IDLE_CONNECTION_WAIT_TIME: "{{ .Values.ATP_ITF_LITE_CLOSE_IDLE_CONNECTION_WAIT_TIME }}"
ATP_ITF_LITE_CONNECTION_TIMEOUT: "{{ .Values.ATP_ITF_LITE_CONNECTION_TIMEOUT }}"
ATP_ITF_LITE_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_DB "def" "atp-itf-lite") }}"
ATP_ITF_LITE_DEFAULT_KEEP_ALIVE_TIME_MILLIS: "{{ .Values.ATP_ITF_LITE_DEFAULT_KEEP_ALIVE_TIME_MILLIS }}"
ATP_ITF_LITE_HTTP_REQUEST_SIZE_MB: "{{ .Values.ATP_ITF_LITE_HTTP_REQUEST_SIZE_MB }}"
ATP_ITF_LITE_HTTP_RESPONSE_SIZE_MB: "{{ .Values.ATP_ITF_LITE_HTTP_RESPONSE_SIZE_MB }}"
ATP_ITF_LITE_MAX_TOTAL_CONNECTIONS: "{{ .Values.ATP_ITF_LITE_MAX_TOTAL_CONNECTIONS }}"
ATP_ITF_LITE_PING_SSE_TIMEOUT: "{{ .Values.ATP_ITF_LITE_PING_SSE_TIMEOUT }}"
ATP_ITF_LITE_REQUEST_TIMEOUT: "{{ .Values.ATP_ITF_LITE_REQUEST_TIMEOUT }}"
ATP_ITF_LITE_SOCKET_TIMEOUT: "{{ .Values.ATP_ITF_LITE_SOCKET_TIMEOUT }}"
ATP_ITF_LITE_SSE_TIMEOUT: "{{ .Values.ATP_ITF_LITE_SSE_TIMEOUT }}"
ATP_NOTIFICATION_MODE: "{{ .Values.ATP_NOTIFICATION_MODE }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: '{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}'
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
CLEANUP_COLLECTION_RUNS_CRON_EXPRESSION: "{{ .Values.CLEANUP_COLLECTION_RUNS_CRON_EXPRESSION }}"
CLEANUP_COOKIES_CRON_EXPRESSION: "{{ .Values.CLEANUP_COOKIES_CRON_EXPRESSION }}"
COLLECTION_RUNS_REMOVE_DAYS: "{{ .Values.COLLECTION_RUNS_REMOVE_DAYS }}"
CLEANUP_SNAPSHOTS_CRON_EXPRESSION: "{{ .Values.CLEANUP_SNAPSHOTS_CRON_EXPRESSION }}"
CLEANUP_SNAPSHOTS_EXPIRATION_PERIOD_SECONDS: "{{ .Values.CLEANUP_SNAPSHOTS_EXPIRATION_PERIOD_SECONDS }}"
CONNECTIONS_PER_HOST: "{{ .Values.CONNECTIONS_PER_HOST }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_HEALTH_CHECK_ENABLED: "{{ .Values.CONSUL_HEALTH_CHECK_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
EI_GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
EI_GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
EI_SERVICE_URL: "{{ .Values.ATP_EXPORT_IMPORT_URL }}"
EI_CLEAN_JOB_ENABLED: "{{ .Values.EI_CLEAN_JOB_ENABLED }}"
EI_CLEAN_JOB_WORKDIR: "{{ .Values.EI_CLEAN_JOB_WORKDIR }}"
EI_CLEAN_SCHEDULED_JOB_PERIOD_MS: "{{ .Values.EI_CLEAN_SCHEDULED_JOB_PERIOD_MS }}"
EI_CLEAN_JOB_FILE_DELETE_AFTER_MS: "{{ .Values.EI_CLEAN_JOB_FILE_DELETE_AFTER_MS }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
FEIGN_ATP_ENVIRONMENTS_NAME: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_NAME }}"
FEIGN_ATP_ENVIRONMENTS_ROUTE: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_ROUTE }}"
FEIGN_ATP_ENVIRONMENTS_URL: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_URL }}"
FEIGN_ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.FEIGN_ATP_INTERNAL_GATEWAY_NAME }}"
FEIGN_ATP_ITF_ENABLED: "{{ .Values.FEIGN_ATP_ITF_ENABLED }}"
FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_NAME: "{{ .Values.FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_NAME }}"
FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_ROUTE: "{{ .Values.FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_ROUTE }}"
FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_URL: "{{ .Values.FEIGN_ATP_ITF_LITE_SCRIPT_ENGINE_URL }}"
FEIGN_ATP_ITF_NAME: "{{ .Values.FEIGN_ATP_ITF_NAME }}"
FEIGN_ATP_ITF_ROUTE: "{{ .Values.FEIGN_ATP_ITF_ROUTE }}"
FEIGN_ATP_ITF_URL: "{{ .Values.FEIGN_ATP_ITF_URL }}"
FEIGN_ATP_MACROS_NAME: "{{ .Values.FEIGN_ATP_MACROS_NAME }}"
FEIGN_ATP_MACROS_ROUTE: "{{ .Values.FEIGN_ATP_MACROS_ROUTE }}"
FEIGN_ATP_MACROS_URL: "{{ .Values.FEIGN_ATP_MACROS_URL }}"
FEIGN_ATP_NOTIFICATION_NAME: "{{ .Values.FEIGN_ATP_NOTIFICATION_NAME }}"
FEIGN_ATP_NOTIFICATION_ROUTE: "{{ .Values.FEIGN_ATP_NOTIFICATION_ROUTE }}"
FEIGN_ATP_NOTIFICATION_URL: "{{ .Values.FEIGN_ATP_NOTIFICATION_URL }}"
FEIGN_ATP_RAM_NAME: "{{ .Values.FEIGN_ATP_RAM_NAME }}"
FEIGN_ATP_RAM_ROUTE: "{{ .Values.FEIGN_ATP_RAM_ROUTE }}"
FEIGN_ATP_RAM_URL: "{{ .Values.FEIGN_ATP_RAM_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
GET_ACCESS_TOKEN_RETENTION_CRON_EXPRESSION: "{{ .Values.GET_ACCESS_TOKEN_RETENTION_CRON_EXPRESSION }}"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
GRID_CHUNK_SIZE: "{{ .Values.GRID_CHUNK_SIZE }}"
GRID_DBNAME: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_GRIDFS_DB "def" "atp-itf-lite-gridfs") }}"
GRID_DICTIONARY_REMOVE_DAYS: "{{ .Values.GRID_DICTIONARY_REMOVE_DAYS }}"
HAZELCAST_CLIENT_ENABLED: "{{ .Values.HAZELCAST_CLIENT_ENABLED }}"
HAZELCAST_CLUSTER_NAME: "{{ .Values.HAZELCAST_CLUSTER_NAME }}"
HAZELCAST_SERVER_ENABLED: "{{ .Values.HAZELCAST_SERVER_ENABLED }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
HISTORY_CLEAN_JOB_EXPRESSION: "{{ .Values.HISTORY_CLEAN_JOB_EXPRESSION }}"
HISTORY_CLEAN_JOB_PAGE_SIZE: "{{ .Values.HISTORY_CLEAN_JOB_PAGE_SIZE }}"
HISTORY_CLEAN_JOB_REVISION_MAX_COUNT: "{{ .Values.HISTORY_CLEAN_JOB_REVISION_MAX_COUNT }}"
HISTORY_RETENTION_CRON_EXPRESSION: "{{ .Values.HISTORY_RETENTION_CRON_EXPRESSION }}"
HISTORY_RETENTION_IN_DAYS: "{{ .Values.HISTORY_RETENTION_IN_DAYS }}"
ITF_LITE_CLEAN_CACHED_FILES: "{{ .Values.ITF_LITE_CLEAN_CACHED_FILES }}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }}-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} -Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false {{ .Values.ADDITIONAL_JAVA_OPTIONS }}"
JAVERS_ENABLED: "{{ .Values.JAVERS_ENABLED }}"
KAFKA_CATALOG_GROUP: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_CATALOG_GROUP "def" "catalog_notification_group") }}"
KAFKA_CATALOG_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_CATALOG_TOPIC "def" "catalog_notification_topic") }}"
KAFKA_ENVIRONMENT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ENVIRONMENT_TOPIC "def" "environments_notification_topic") }}"
KAFKA_ITF_LITE_EXECUTION_FINISH_PARTITIONS: "{{ .Values.KAFKA_ITF_LITE_EXECUTION_FINISH_PARTITIONS }}"
KAFKA_ITF_LITE_EXECUTION_FINISH_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_ITF_LITE_EXECUTION_FINISH_REPLICAS) }}"
KAFKA_ITF_LITE_EXECUTION_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXECUTION_FINISH_TOPIC "def" "itf_lite_execution_finish") }}"
KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_ITF_FINISH_TOPIC "def" "itf_lite_to_itf_export_finish") }}"
KAFKA_ITF_LITE_EXPORT_ITF_PARTITIONS: "{{ .Values.KAFKA_ITF_LITE_EXPORT_ITF_PARTITIONS }}"
KAFKA_ITF_LITE_EXPORT_ITF_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_ITF_LITE_EXPORT_ITF_REPLICAS) }}"
KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_ITF_START_TOPIC "def" "itf_lite_to_itf_export_start") }}"
KAFKA_ITF_LITE_EXPORT_MIA_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_MIA_FINISH_TOPIC "def" "itf_lite_to_mia_export_finish") }}"
KAFKA_ITF_LITE_EXPORT_MIA_PARTITIONS: "{{ .Values.KAFKA_ITF_LITE_EXPORT_MIA_PARTITIONS }}"
KAFKA_ITF_LITE_EXPORT_MIA_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_ITF_LITE_EXPORT_MIA_REPLICAS) }}"
KAFKA_ITF_LITE_EXPORT_MIA_START_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_EXPORT_MIA_START_TOPIC "def" "itf_lite_to_mia_export_start") }}"
KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_PARTITIONS: "{{ .Values.KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_PARTITIONS }}"
KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_REPLICAS) }}"
KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_LITE_GET_ACCESS_TOKEN_FINISH_TOPIC "def" "itf_lite_get_access_token_finish") }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC "def" "orch_logrecord_context_topic") }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_CONTEXT_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_SCRIPTS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_SCRIPTS_TOPIC "def" "orch_logrecord_scripts_topic") }}"
KAFKA_LOGRECORD_SCRIPTS_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_SCRIPTS_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_SCRIPTS_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_SCRIPTS_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_LOGRECORD_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_LOGRECORD_TOPIC "def" "orch_logrecord_topic") }}"
KAFKA_LOGRECORD_TOPIC_PARTITIONS: "{{ .Values.KAFKA_LOGRECORD_TOPIC_PARTITIONS }}"
KAFKA_LOGRECORD_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_LOGRECORD_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_NOTIFICATION_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_NOTIFICATION_TOPIC "def" "notification_topic") }}"
KAFKA_NOTIFICATION_TOPIC_MIN_INSYNC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_NOTIFICATION_TOPIC_MIN_INSYNC_REPLICATION_FACTOR) }}"
KAFKA_NOTIFICATION_TOPIC_PARTITIONS: "{{ .Values.KAFKA_NOTIFICATION_TOPIC_PARTITIONS }}"
KAFKA_NOTIFICATION_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_NOTIFICATION_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_REPORTING_SERVERS: '{{ .Values.KAFKA_REPORTING_SERVERS }}'
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service_entities") }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
LOG_LEVEL: "{{ .Values.LOG_LEVEL }}"
MAX_CONNECTION_IDLE_TIME: "{{ .Values.MAX_CONNECTION_IDLE_TIME }}"
MAX_RAM: "{{ .Values.MAX_RAM }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MIN_CONNECTIONS_PER_HOST: "{{ .Values.MIN_CONNECTIONS_PER_HOST }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
SERVICE_ENTITIES_MIGRATION_ENABLED: "{{ .Values.SERVICE_ENTITIES_MIGRATION_ENABLED }}"
SERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
ATP_ITF_LITE_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_DB_PASSWORD "def" "atp-itf-lite") }}"
ATP_ITF_LITE_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_DB_USER "def" "atp-itf-lite") }}"
ATP_ITF_LITE_GRIDFS_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_GRIDFS_DB_PASSWORD "def" "atp-itf-lite-gridfs") }}"
ATP_ITF_LITE_GRIDFS_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.ATP_ITF_LITE_GRIDFS_DB_USER "def" "atp-itf-lite-gridfs") }}"
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-itf-lite" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "290238dd-5e4b-40ff-9b18-8b5ebcd628d5" .Values.KEYCLOAK_SECRET }}"
{{- end }}

{{- define "env.deploy" }}
ei_gridfs_pass: "{{ .Values.ei_gridfs_pass }}"
ei_gridfs_user: "{{ .Values.ei_gridfs_user }}"
gridfs_pass: "{{ .Values.gridfs_pass }}"
gridfs_user: "{{ .Values.gridfs_user }}"
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
{{- end }}

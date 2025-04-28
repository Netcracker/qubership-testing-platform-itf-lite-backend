#!/usr/bin/env sh

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

# shellcheck source=../atp-common-scripts/openshift/common.sh
. ./atp-common-scripts/openshift/common.sh

#NAMESPACE is used by cloud-deployer, OPENSHIFT_PROJECT is for DP deployer
_ns="${NAMESPACE}"

ATP_ITF_LITE_DB="$(env_default "${ATP_ITF_LITE_DB}" "atp-itf-lite" "${_ns}")"
ATP_ITF_LITE_DB_USER="$(env_default "${ATP_ITF_LITE_DB_USER}" "atp-itf-lite" "${_ns}")"
ATP_ITF_LITE_DB_PASSWORD="$(env_default "${ATP_ITF_LITE_DB_PASSWORD}" "atp-itf-lite" "${_ns}")"
ATP_ITF_LITE_GRIDFS_DB="$(env_default "${ATP_ITF_LITE_GRIDFS_DB}" "atp-itf-lite-gridfs" "${_ns}")"
ATP_ITF_LITE_GRIDFS_DB_USER="$(env_default "${ATP_ITF_LITE_GRIDFS_DB_USER}" "atp-itf-lite-gridfs" "${_ns}")"
ATP_ITF_LITE_GRIDFS_DB_PASSWORD="$(env_default "${ATP_ITF_LITE_GRIDFS_DB_PASSWORD}" "atp-itf-lite-gridfs" "${_ns}")"
EI_GRIDFS_DB="$(env_default "${EI_GRIDFS_DB}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_USER="$(env_default "${EI_GRIDFS_USER}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_PASSWORD="$(env_default "${EI_GRIDFS_PASSWORD}" "atp-ei-gridfs" "${_ns}")"

init_pg "${PG_DB_ADDR}" "${ATP_ITF_LITE_DB}" "${ATP_ITF_LITE_DB_USER}" "${ATP_ITF_LITE_DB_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"

init_mongo "${GRIDFS_DB_ADDR}" "${ATP_ITF_LITE_GRIDFS_DB}" "${ATP_ITF_LITE_GRIDFS_DB_USER}" "${ATP_ITF_LITE_GRIDFS_DB_PASSWORD}" "${GRIDFS_DB_PORT}" "${gridfs_user}" "${gridfs_pass}"

init_mongo "${EI_GRIDFS_DB_ADDR:-$GRIDFS_DB_ADDR}" "${EI_GRIDFS_DB}" "${EI_GRIDFS_USER}" "${EI_GRIDFS_PASSWORD}" "${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}" "${ei_gridfs_user:-$gridfs_user}" "${ei_gridfs_pass:-$gridfs_pass}"

echo "***** Setting up encryption *****"
encrypt "${ENCRYPT}" "${SERVICE_NAME}"

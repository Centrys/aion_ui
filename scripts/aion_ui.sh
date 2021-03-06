#!/bin/bash

# get the directory of the currently executing script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done

SCRIPT_PATH="$( cd -P "$( dirname "$SOURCE" )" >/dev/null && pwd )"
echo "Located script directory: ${SCRIPT_PATH}"
cd ${SCRIPT_PATH}

# setup other directories
STORAGE_DIR=${HOME}/.aion
LOG_DIR=${STORAGE_DIR}/log
JAVA_INSTALL=${STORAGE_DIR}/jre-10.0.2
JAVA_CMD=${JAVA_INSTALL}/bin/java

if [ ! -f ${JAVA_CMD} ] || [ $(${JAVA_CMD} -version 2>&1 | grep "10.0.2" | wc -l) -lt 1 ]; then
  mkdir -p ${JAVA_INSTALL}
  cp -r rt/linux/* ${JAVA_INSTALL}
fi

MOD_DIR=${SCRIPT_PATH}/mod/*
LIB_DIR=${SCRIPT_PATH}/lib/*

mkdir -p ${LOG_DIR}

${JAVA_CMD} -cp "${MOD_DIR}:${LIB_DIR}" -Dlocal.storage.dir=${STORAGE_DIR} -Xms300m -Xmx500m org.aion.wallet.WalletApplication &> ${LOG_DIR}/log &

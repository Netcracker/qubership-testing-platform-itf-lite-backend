FROM bellsoft/liberica-openjdk-alpine-musl:21.0.11

LABEL maintainer="opensourcegroup@netcracker.com" \
      qubership.atp.service="atp-itf-lite-backend"

ENV HOME_EX=/atp-itf-lite
ENV GRID_DBNAME=atpitflite
ENV GRIDFS_DB_ADDR=localhost
ENV GRIDFS_DB_PORT=27017

WORKDIR $HOME_EX

RUN echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/community/" >/etc/apk/repositories && \
    echo "https://dl-cdn.alpinelinux.org/alpine/v3.23/main/" >>/etc/apk/repositories && \
    apk add --update --no-cache --no-check-certificate \
            bash \
            curl \
            font-dejavu \
            fontconfig \
            gcompat \
            gettext \
            git \
            htop \
            jq \
            libpng \
            libcrypto3 \
            libexpat \
            libssl3 \
            musl \
            musl-utils \
            net-tools \
            nss_wrapper \
            pcre2 \
            procps-ng \
            sops \
            sysstat \
            tcpdump \
            wget \
            xz-libs \
            zip \
            zlib && \
        rm -rf /var/cache/apk/*

COPY deployments/install deployments/install
COPY deployments/atp-common-scripts deployments/atp-common-scripts
COPY build-context/atp-itf-lite-backend-distribution/target/ /tmp/

RUN mkdir -p dist/atp deployments/update && \
    cp -r deployments/install/* deployments/update/ && \
    find deployments -maxdepth 1 -regex '.*/\(install\|update\|atp-common-scripts\)$' -exec mv -t dist/atp {} +

RUN adduser -D -H -h /atp -s /bin/bash -u 1007 atp && \
    mkdir -p /etc/env /etc/alternatives /tmp/log/diagnostic /tmp/cert && \
    ln -s ${JAVA_HOME}/bin/java /etc/alternatives/java && \
    echo "${JAVA_HOME}/bin/java \$@" >/usr/bin/java && \
    chmod a+x /usr/bin/java

RUN unzip /tmp/atp-itf-lite-backend-distribution-*.zip -d $HOME_EX/ && \
    cp -r dist/atp /atp/ && chmod -R 775 /atp/ && \
    chown -R atp:root $HOME_EX/ && \
    find $HOME_EX -type f -name '*.sh' -exec chmod a+x {} + && \
    find $HOME_EX -type d -exec chmod 777 {} \;

RUN find $HOME_EX -type f -name "*-sources.jar" -exec rm -f {} \;

EXPOSE 8080
USER atp

CMD ["./run.sh"]

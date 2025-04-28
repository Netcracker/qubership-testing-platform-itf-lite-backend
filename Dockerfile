FROM artifactory-service-address/path-to-java-image

LABEL maintainer="our-team@qubership.org"
LABEL atp.service="atp-itf-lite-backend"

ENV HOME_EX=/atp-itf-lite
ENV GRID_DBNAME=atpitflite
ENV GRIDFS_DB_ADDR=localhost
ENV GRIDFS_DB_PORT=27017

WORKDIR $HOME_EX

COPY --chmod=775 dist/atp /atp/
COPY --chown=atp:root build $HOME_EX/

RUN find $HOME_EX -type f -name '*.sh' -exec chmod a+x {} + && \
    find $HOME_EX -type d -exec chmod 777 {} \;

EXPOSE 8080

USER atp

CMD ["./run.sh"]

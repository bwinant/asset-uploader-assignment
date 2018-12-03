FROM library/openjdk:11-jdk-slim

COPY target/asset-uploader.jar /opt/asset-uploader.jar

EXPOSE 8080

ARG ASSET_BUCKET
ARG REGION
ARG AWS_ACCESS_KEY_ID
ARG AWS_SECRET_ACCESS_KEY

ENV ASSET_BUCKET=${ASSET_BUCKET} \
    REGION=${REGION} \
    AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
    AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}

# Would set -Xms, -Xmx, etc if were interested in seriously tuning the JVM
CMD ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED", "-jar", "/opt/asset-uploader.jar"]
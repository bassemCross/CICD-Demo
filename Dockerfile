FROM registry2.swarm.devfactory.com/aurea/sonic/releases:sonic-domain-manager-12.0.1

# What SDM model, environment and hosts should be deployed in this image?
ENV ENVIRONMENT_ID=Docker \
    LOGICAL_HOSTS=dshost,XARHost \
    SDM_DIR=/Model

# How can the container management scripts connect to the deployed domain?
ENV DOMAIN_NAME=dmNCA \
    DS_CONNECTION_URLS=tcp://localhost:2506 \
    DS_USERNAME=Administrator \
    DS_PASSWORD=Administrator

# Installs ps and vi packages for debugging. This is optional.
RUN apt install procps vim -y

# Add the Messenger deployment model to the container image.
RUN mkdir -p /Model
COPY deploy/*.xar /Model/

# Document ports to publish for access from outside the container.
EXPOSE 2506

# Deploy the Messenger model and configure the image to auto start
# the newly deployed domain.
RUN $SONIC_INSTALLER_HOME/conf/run.sh setup

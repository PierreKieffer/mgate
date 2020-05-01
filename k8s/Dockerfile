FROM ubuntu:16.04

RUN  apt-get update \
  && apt-get install -y wget \
  && rm -rf /var/lib/apt/lists/*
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

RUN apt-get update
RUN apt-get install -y vim
RUN apt-get install -y curl 

WORKDIR /
RUN mkdir /usr/local/config
RUN mkdir /usr/local/data-model
RUN mkdir /logs
RUN mkdir /usr/local/keys
RUN chmod 777 -R /logs
RUN chmod 777 -R /usr/local/config
RUN chmod 777 -R /usr/local/data-model
WORKDIR /usr/local

ADD mgate-assembly-0.1.jar /usr/local/

CMD java -cp mgate-assembly-0.1.jar API.akkaServer config/config.yml

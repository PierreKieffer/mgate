#!/bin/bash
PROJECT=$1
CLUSTER=$2
FILE=$3

if [ -z "$PROJECT" ]; then
        echo "Usage ./generate-configMap.sh PROJECT_ID CLUSTER FILE "
        exit 1

fi

if [ -z "$CLUSTER" ]; then
        echo "Usage ./generate-configMap.sh PROJECT_ID CLUSTER FILE "
        exit 1

fi

if [ -z "$FILE" ]; then
        echo "Usage ./generate-configMap.sh PROJECT_ID CLUSTER FILE "
        exit 1

fi

gcloud config set project $PROJECT
gcloud container clusters get-credentials $CLUSTER
kubectl delete configmap schema-registry-config-production
kubectl create configmap schema-registry-config-production --from-file $FILE




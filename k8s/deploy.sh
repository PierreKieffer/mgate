
#/bin/bash

PROJECT=$1
CLUSTER=$2

echo """

 ____  _____ _____ __    _____ __ __ _____ _____ _____ _____
|    \|   __|  _  |  |  |     |  |  |     |   __|   | |_   _|
|  |  |   __|   __|  |__|  |  |_   _| | | |   __| | | | | |
|____/|_____|__|  |_____|_____| |_| |_|_|_|_____|_|___| |_|

  Cluster k8s : $CLUSTER
  Projet : $PROJECT

"""

if [ -z "$PROJECT" ]; then
        echo "You have to pass PROJECT_ID as arg"
        echo "Usage ./deploy PROJECT_ID CLUSTER_ID"
        exit 1

fi

if [ -z "$CLUSTER" ]; then
        echo "You have to pass CLUSTER_ID as arg"
        echo "Usage ./deploy PROJECT_ID CLUSTER_ID"
        exit 1

fi

gcloud config set project $PROJECT
gcloud container clusters get-credentials $CLUSTER
kubectl apply -f deployment.yml


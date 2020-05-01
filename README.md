# mGate 

```

                               
 _____ _____ _____ _____ _____ 
|     |   __|  _  |_   _|   __|
| | | |  |  |     | | | |   __|
|_|_|_|_____|__|__| |_| |_____|
                               

```
                               
mGate is a schema validation layer for mongodb, which provides : 
- Metadata management rest interface 
- Schema validation for insert and update/upsert queries 
- Production deployment workflow on kubernetes

## Run 

### configuration 
You need to edit the config.yml file with your own settings : 

You must specify the names of the schemas assigned to each collection, and the database names of each collection
```yaml 
associatedCollectionSchema : '{"collection1" : "schema_name1","collection2":"schema_name2"}'
associatedCollectionDatabase : '{"collection1":"database_name","collection2":"database_name"}'
```

### Start mGate 
```bash 
docker run --network host config.yml:/usr/local/config/config.yml mgate 
```

## Usage 
### Upload or update a schema
Your schema name must respect the following pattern : name_version.json (user_v2.json)

Your schema must respect json standard rules (http://json-schema.org/draft-04/schema#)

To easily generate json schemas, you can refer to https://github.com/sauldhernandez/autoschema 

To upload a new schema or update an existing schema : 

`./admin-scripts/deploy.sh schema-name version`

### Routes 
- Main

```
http://<HOST>:8080/secured/schema-registry
```

- Test auth

```
/test-login
```

- INSERT 

```
/db-driver/insert
```

Payload : 
```json 
{
    "collection": "users",
    "document": $jsonDocument
}
```
jsonDocument = { json associated to collection data}

Example : 
```json  
{
    "collection": "users",
    "document": {
        "email": "pikachu@mail.com",
        "firstName": "Pika",
        "lastName": "Chu",
        "communications": {
            "allowNewsletters": False
        }
    }
}
```
```bash 
curl -X POST -d @insert-data.json http://localhost:8080/secured/schema-registry/db-driver/insert --header "Content-Type:application/json" --header "Authorization: Bearer token"
```


- UPDATE 

```
/db-driver/update
```

Payload : 

```json 
{
    "collection":  String,  	    
    "_id" : String,    
    "document": $jsonDocument
}
```

jsonDocument = { json data to update}

Example : 

```json 
{
    "collection": "users",
    “_id” : “592be28bf322cc152069e957”
    "document": {
        "email": "pikachu-newmail@mail.com",
        "communications": {
            "allowNewsletters": True, 
            "allowSMS": True
            }
         }
}
```

```bash 
curl -X POST -d @update-data.json http://localhost:8080/secured/schema-registry/db-driver/update --header "Content-Type:application/json" --header "Authorization: Bearer token"
```
It is possible to pass MongoDB operations inside the document : 

Example : 
```json 
"document" : {"array" : {"$each" : ["foo","bar"] }}
```

- UPSERT 

```
/db-driver/upsert
```
Payload :

```json 
{
    "collection":  String,
    "_id" : String,
    "document": $jsonDocument
}
```

jsonDocument = { json data to update}

Example :

```json 
{
    "collection": "users",
    “_id” : “592be28bf322cc152069e957”
    "document": {
        "email": "pikachu-newmail@mail.com",
        "communications": {
            "allowNewsletters": True,
            "allowSMS": True
            }
         }
}
```


## Build and deploy 
### Package application 
To package the application you need sbt 0.13.15

```bash 
sbt assembly
```


### Docker

```bash 
docker build -t mgate . 
``` 

### Build and deploy on Kubernetes 
#### Prerequisities 
You will need to install : 
- gcloud 
- kubectl 


#### Deploying to kubernetes 
##### Application
Build docker image and push it to your favorite registry

Add your own parameters to  the config.yml file

Generate the config-map :

```bash 
./k8s/generate-configMap.sh PROJECT_ID CLUSTER_ID config.yml
```

Deploy :

```bash 
./deploy.sh PROJECT_ID CLUSTER_ID
``` 


 

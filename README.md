# RuDiK-GUI
A GUI for RuDiK which is a system for discovering positive and negative logical rules over RDF knowledge graphs.<br>
For more details about the project please visit: https://github.com/stefano-ortona/rudik.

The following GUI is a <a href="https://www.dropwizard.io/1.3.5/docs/">Dropwizard </a> application and therefore follows a Dropwizard application structure. 

## Getting started
These instructions will get you a copy of the project up and running on your local machine for testing purposes.
Please note that some browsers like ```Mozilla Firefox``` and ```Microsoft Edge``` do not support the tools used to build the GUI. Therefore, to have a good experience with this verison of the GUI, please use ```Google Chrome```.

### STEP 1: 
Clone the git project: ```git clone https://github.com/lionelsome/RuDiK-GUI.git```;

### STEP 2: 
Build the project. Inside the project folder: run ```mvn -U clean package -DskipTests```;

### STEP 3: 
The following Web application works primarily with Virtuoso installed locally and available at localhost:8890/sparql (the parameter "Sparql endpoint" allows to choose the endpoint we want to query). Therefore, make sure the Virtuoso server is started (following the previous guide):
 ```cd /var/lib/virtuoso-opensource-6.1/db
 sudo /usr/bin/virtuoso-t -f 
 ```
 
### STEP 4: 
Compile and laugh the application. Inside the project folder run:
```java -jar target/rule_miner-0.0.1-SNAPSHOT.jar server rudik.yml``` where rudik.yml is the application’s configuration file. 
In your web browser, go to ```localhost:9090/rudik``` and start the adventure with Rudik!

## How to use it ?

### Discover new rules : 
It allows to run RuDiK against the selected knowledge base in order to find new rules related to the selected predicate.
Select first the ```Type of rule``` and then the ```Knowledge Graph``` (KG). Depending on the KG, a list of predicates are suggested : you can either select one of them or enter another predicate that exists in the KG in ```Target predicate```(since no check is made on the correctness of the typed predicate, make sure it does exist).<br>
In the ```More options```, select the ```Sparql endpoint``` you want to use based on your installation (whether you have Virtuoso installed or not) and the KG that you selected. There are other parameter that can be changed there. Please refer to github mentioned above for more details.<br>
Click on ```Run``` in the ```Main parameters``` card and wait for RuDiK to make the job. <br>
You can see the Generation and Validation set (refer to the technical report for more details) of examples of each rule and also a Sankey diagram with all the rules and their corresponding Generation and validation Set. For each example, the ```Graph``` button allows to show a surrounding graph.
This GUI also gives you the possibility to instantiate the discovered rules. 

N.B: You can hover over each parameter to get a description.

### Instantiate a rule
Select a KG and the corresponding ```Sparql endpoint```. According to the selected KG, a list of rules to instantiate are suggested. Select one of them or enter your own rule you want to instantiate.<br>
Here, you can also have a surrounding graph for the literals involved in the instantiation. 

## Built With

* [Dropwizard](http://www.dropwizard.io/1.3.5/docs/) (1.3.5 or above) - The web framework used
* [Maven](https://maven.apache.org/) (3.2 or above) - Dependency Management

## Acknowledgments

* Stefano Ortona
* Prof. Paolo Papotti

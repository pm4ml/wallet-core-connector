# mojaloop-wallet-core-connector
Sample project for a Mojaloop core connector (client adapter) for a wallet provider core banking system

to generate the Java Rest DSL router and Model files (In parent pom): mvn clean install

To Build the Project: mvn clean package

To Build the project using Docker: docker build -t wallet-core-connector .

To run the project using Docker: docker run -p 3001:3001 -p 8080:8080 -t wallet-core-connector

To run the Integration Tests (run mvn clean install under core-connector folder first): mvn -P docker-it clean install

Architecture diagram: 

![Alt text](diagram.jpg?raw=true "Integration Architecture")

# Production-Grade Order Integration Platform Using Apache Camel

## Scenario

An e-commerce organization receives orders from multiple sources, including REST APIs, CSV files, EDI trading partners, and SFTP servers.

The Production-Grade Order Integration Platform is developed using Spring Boot and Apache Camel to automate order processing, validate incoming data, persist orders, and route messages between different enterprise systems.

The application demonstrates real-time enterprise integration patterns with database integration, messaging, file processing, error handling, duplicate prevention, and reliable message processing.

## Technologies Used

- Java 17
- Spring Boot
- Apache Camel 4.22.0
- Spring Data JPA
- H2 Database
- MySQL
- Apache ActiveMQ Artemis
- Apache Kafka
- REST APIs
- SFTP
- EDI / X12
- Smooks
- Maven
- Postman

## Application Architecture

```text
       External Systems
              |
      +-------+--------+
      |       |        |
     File    REST     SFTP
      |       |        |
      +-------+--------+
              |
              v
       Apache Camel
              |
              v
       Data Validation
              |
              v
       Business Processing
              |
              v
         Database
              |
              v
      ActiveMQ Artemis
              |
              v
       Message Consumer
              |
              v
          Kafka
              |
              v
      Downstream Systems
```

The platform uses Apache Camel routes to connect different systems while keeping integration logic separate from business logic.

## Project Structure

```text
ProductionGradeCamelIntegration/
├── src/main/java/
│   └── com.example.orderintegration/
│       ├── route/
│       │   ├── OrderFileRoute.java
│       │   ├── OrderRestRoute.java
│       │   ├── OrderMessageConsumerRoute.java
│       │   ├── OrderKafkaRoute.java
│       │   ├── OrderSftpRoute.java
│       │   └── EdiPurchaseOrderRoute.java
│       │
│       ├── processor/
│       ├── bean/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── exception/
│       └── config/
│
├── src/main/resources/
│   └── application.properties
│
├── pom.xml
└── README.md
```

## Implemented Integration Scenarios

### 1. File-Based Order Processing

Processes incoming CSV order files using Apache Camel.

- Polls incoming files.
- Validates order data.
- Processes valid records.
- Stores orders in the database.
- Routes invalid records to the error location.
- Moves successfully processed files to the processed location.

### 2. REST API Integration

Exposes REST endpoints to receive order information from external applications.

- Accepts incoming order requests.
- Validates request data.
- Processes orders through Camel routes.
- Integrates with database and messaging systems.

### 3. Database Integration

Uses Spring Data JPA and H2 to persist order information.

- Stores validated orders.
- Checks for duplicate order IDs.
- Retrieves existing order information.
- Maintains order processing status.

### 4. ActiveMQ Artemis Integration

Uses ActiveMQ Artemis for asynchronous order processing.

- Publishes processed orders to `order.queue`.
- Consumes messages from the queue.
- Separates message production from consumption.
- Supports asynchronous communication between integration components.

### 5. Kafka Integration

Integrates Apache Kafka for event-based order distribution.

- Publishes order events to Kafka topics.
- Supports downstream event consumption.
- Demonstrates Camel-based Kafka producer and consumer integration.

### 6. EDI Purchase Order Integration

Processes electronic purchase orders received from external trading partners.

- Receives X12 purchase order data.
- Parses EDI data using Smooks.
- Converts EDI data into a structured format.
- Validates purchase order information.
- Processes the resulting order through the integration platform.

### 7. SFTP Integration

Receives order files from external systems through SFTP.

- Polls the SFTP server for incoming files.
- Retrieves and processes order data.
- Validates incoming records.
- Handles duplicate orders.
- Moves files to appropriate processing locations.

## End-to-End Order Processing Flow

```text
External Order Source
        |
        v
File / REST / EDI / SFTP
        |
        v
Apache Camel Route
        |
        v
Generate Correlation ID
        |
        v
Validate Order Data
        |
        +------ Invalid ------> Error Handling
        |                           |
        v                           v
Business Processing           Error Location
        |
        v
Duplicate Order Check
        |
        v
Database Persistence
        |
        v
ActiveMQ Artemis
        |
        v
Message Consumer
        |
        v
Kafka Integration
        |
        v
Downstream Systems
```

## Key Implementations

### 1. Apache Camel Routing

Implemented Camel routes using different components to connect files, REST APIs, databases, messaging systems, Kafka, and SFTP.

### 2. Validation Framework

Implemented reusable validation logic to verify incoming order data before business processing.

### 3. Duplicate Order Prevention

Implemented duplicate checking using order IDs and database constraints to prevent repeated order processing.

### 4. Error Handling

Implemented error handling to manage validation failures and processing exceptions.

- Routes invalid records to error locations.
- Handles processing failures.
- Supports controlled message processing and error recovery.

### 5. Correlation ID

Uses a unique correlation ID to identify and trace an order throughout the integration flow.

The correlation ID is stored in message headers and used in application logs to help troubleshoot failures.

### 6. Database Integration

Uses Spring Data JPA to persist order information and retrieve existing records.

### 7. Asynchronous Messaging

Uses ActiveMQ Artemis to decouple order processing from downstream consumers.

### 8. Event-Based Integration

Uses Kafka to publish order events for downstream processing.

### 9. EDI Processing

Uses Smooks to parse X12 purchase order messages and transform them into structured data for further processing.

### 10. SFTP File Transfer

Uses Apache Camel SFTP integration to receive and process files from external systems.

## Configuration

Application properties are used to configure database connections, messaging endpoints, file locations, and integration settings.

Example:

```properties
spring.datasource.url=jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update

camel.main.run-controller=true
```

External system configurations such as Artemis, Kafka, and SFTP are maintained separately from the route logic.

## How to Run

1. Clone the repository.

```bash
git clone https://github.com/puspanjalimahanty/ProductionGradeCamelIntegrationFlow.git
```

2. Open the project in Eclipse or IntelliJ IDEA.

3. Configure the required database and messaging services.

4. Start Apache ActiveMQ Artemis.

5. Start Apache Kafka and ensure the required topics are available.

6. Configure the SFTP server and required directories.

7. Run the Spring Boot application.

8. Submit sample orders through REST APIs or incoming files.

9. Monitor the Camel routes and application logs.

## Expected Outcomes

- Orders are received from multiple external systems.
- Incoming order data is validated before processing.
- Valid orders are persisted in the database.
- Duplicate orders are identified and handled.
- Orders are published to ActiveMQ Artemis.
- Messages are consumed and integrated with Kafka.
- EDI purchase orders are parsed and processed.
- SFTP files are received and processed automatically.
- Processing failures are handled through error routes.
- Correlation IDs help trace orders across integrations.

## Learning Outcomes

- Building enterprise integration applications using Apache Camel.
- Implementing File, REST, Database, ActiveMQ, Kafka, and SFTP integrations.
- Understanding Camel routes and Enterprise Integration Patterns.
- Implementing reusable validation and error handling.
- Preventing duplicate message processing.
- Understanding asynchronous messaging and event-driven architecture.
- Processing EDI X12 purchase orders.
- Implementing correlation IDs for message tracing.
- Integrating Apache Camel with Spring Boot and Spring Data JPA.

## Author

**Puspanjali Mahanty**

GitHub: [puspanjalimahanty](https://github.com/puspanjalimahanty)

# UnravelDocs API File Extraction and Management Platform

UnravelDocs is a comprehensive document processing and management platform designed to help users extract insights from their documents. It offers features like OCR, AI-powered analysis (planned), and secure document storage. The platform supports subscription-based access with multiple payment gateway integrations (Stripe, PayPal, Flutterwave, Paystack) and multi-currency options. It includes robust administrative functionalities for user management, subscription plan management, document oversight, and system monitoring. The application is built with internationalization in mind and features detailed user activity and admin action logging for security and auditing.

## Features

*   **Secure Document Upload and Storage:** Safely upload and manage your documents.
*   **OCR Processing:** Extract text from images and scanned documents.
*   **AI-Powered Document Analysis:** (Planned) Capabilities like entity extraction, classification, and summarization.
*   **Subscription Management:** Tiered subscription plans with varying features and limits.
*   **Payment Gateway Integrations:**
    *   Stripe
    *   PayPal
    *   Flutterwave
    *   Paystack
*   **Multi-Currency Support:** Allow users to pay in their preferred or local currencies.
*   **User Account Management:** User registration, login, profile management.
*   **Role-Based Access Control (RBAC):** Differentiated access for regular users and administrators.
*   **Comprehensive Admin Panel APIs:**
    *   **User Management:** View user details, manage status (active/inactive), and roles.
    *   **Subscription Plan Management:** Create, view, update, and activate/deactivate subscription plans.
    *   **Document Overview and Management:** List, view details, and delete documents (for moderation/support).
    *   **System Statistics and Monitoring:** View key metrics on users, documents, and subscriptions.
*   **Internationalization (i18n):** Support for multiple languages and regional formats.
*   **User Activity Logging:** Track significant actions performed by users for auditing and support.
*   **Admin Action Audit Logging:** Log actions performed by administrators for accountability.

## Tech Stack

*   **Backend:** Java, Spring Boot (Spring Web, Spring Security, Spring Data JPA)
*   **Build Tool:** Apache Maven
*   **Database:** PostgreSQL
*   **Database Migrations:** Flyway
*   **Authentication:** JWT (JSON Web Tokens)
*   **Payment Integrations:** Stripe SDK, PayPal SDK, Flutterwave API, Paystack API
*   **(Potential Frontend):** (To be determined - APIs are designed to support various clients like SPAs or mobile apps)

## Prerequisites

*   JDK 17 or higher
*   Apache Maven 3.6.x or higher
*   A running SQL database instance - PostgreSQL
*   API keys and credentials for desired payment gateways (Stripe, PayPal, Flutterwave, Paystack) if you intend to test payment functionalities.

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository_url>
cd UnravelDocs
```
### 2. Configure Database
Create a PostgreSQL database and user, then configure the `application.properties` file in the `src/main/resources` directory with your database connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/unravel_docs
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
```
### 3. Configure Payment Gateways
If you plan to use payment functionalities, configure the payment gateway settings in the `application.properties` file:

```properties
# Stripe
stripe.api.key=your_stripe_api_key
# PayPal
paypal.client.id=your_paypal_client_id
paypal.client.secret=your_paypal_client_secret
# Flutterwave
flutterwave.api.key=your_flutterwave_api_key
# Paystack
paystack.api.key=your_paystack_api_key
```
### 4. Build the Project
Run the following command to build the project:

```bash
mvn clean install
```
### 5. Run the Application
You can run the application using the following command:

```bash
mvn spring-boot:run
```
### 6. Access the Application
Open your web browser and navigate to `http://localhost:8080` to access the application. You can use tools like Postman or cURL to interact with the APIs.
### 7. Database Migration
Run the following command to apply database migrations:

```bash
mvn flyway:migrate
```
### 8. Testing
You can run the unit and integration tests using:

```bash
mvn test
```
### 9. Test Single File
```bash
mvn test -Dtest=FileProcessingServiceTest#testProcessSingleFile
```
### 10. Test Multiple Files
```bash
mvn test -Dtest=FileProcessingServiceTest#testProcessMultipleFiles
```
### 11. Test with Coverage Report
```bash
mvn clean test jacoco:report
```
### 12. Documentation
The API documentation is available in the `src/main/resources/static/docs` directory. You can view it by opening the `index.html` file in a web browser.

### 13. Start Redis Server using Docker
If your application uses Redis for caching or session management, you can start a Redis server using Docker with the following command:
```bash
docker run --name redis-unraveldocs -p 6379:6379 -d redis
```

## Configure the Application
You can configure various aspects of the application in the `application.properties` file located in the `src/main/resources` directory. This includes database settings, payment gateway configurations, and other application-specific properties.
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/unraveldocs_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=validate # Recommended: Flyway handles schema
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server Configuration
server.port=8080
server.servlet.context-path=/unraveldocs

# You can use the following command to generate a strong secret key:
# openssl rand -base64 32

# JWT Configuration
jwt.secret=your-very-strong-and-long-jwt-secret-key-min-256-bits
jwt.expiration.ms=86400000
```
# Bank Statement Service

REST service for importing and exporting bank statements via CSV and
calculating account balances.

Built with **Java 21** and **Spring Boot**.

------------------------------------------------------------------------

## Features

-   Import bank statements from CSV\
-   Export statements for one or several accounts\
-   Calculate account balance for a given account and date range\
-   CSV validation and clear error handling\
-   Duplicate prevention\
-   Database migrations with Flyway\
-   Swagger / OpenAPI documentation\
-   Unit and integration tests

------------------------------------------------------------------------

## Tech stack

-   Java 21\
-   Spring Boot\
-   Spring Data JPA\
-   H2 in-memory database\
-   Flyway\
-   Apache Commons CSV\
-   Springdoc OpenAPI\
-   JUnit 5, Mockito, JaCoCo

------------------------------------------------------------------------

## API

### Import CSV

`POST /api/v1/statements/import`

Expected CSV header:

    accountNumber,operationDateTime,beneficiary,comment,amount,currency

Returns: - `imported` -- number of saved records\
- `skippedDuplicates` -- number of skipped duplicate records

------------------------------------------------------------------------

### Export CSV

`GET /api/v1/statements/export`

Exports statements for **one or several accounts**.

Query parameters: - `accounts` -- required\
- `from` -- optional (`yyyy-MM-dd`)\
- `to` -- optional (`yyyy-MM-dd`)

------------------------------------------------------------------------

### Calculate balance

`GET /api/v1/statements/accounts/{accountNumber}/balance`

Parameters: - `accountNumber` -- mandatory\
- `from` -- optional (`yyyy-MM-dd`)\
- `to` -- optional (`yyyy-MM-dd`)

------------------------------------------------------------------------

## Example curl

### Import

``` bash
curl -X POST http://localhost:8080/api/v1/statements/import   -F "file=@samples/import-sample.csv"
```

### Export

``` bash
curl -X GET "http://localhost:8080/api/v1/statements/export?accounts=LT100001&from=2025-01-02&to=2025-01-05" -o statements.csv
```

### Balance

``` bash
curl -X GET "http://localhost:8080/api/v1/statements/accounts/LT100001/balance?from=2025-01-01&to=2025-01-31"
```

------------------------------------------------------------------------

## Sample files

Example CSV files are available in the `samples/` folder: 
- `import-sample.csv`\
- `import-missing-header.csv`\
- `import-invalid-date.csv`\
- `import-multi-currency.csv`

------------------------------------------------------------------------

## Validation and error handling

The service validates: - CSV format and required headers\
- Date ranges (`from` must not be after `to`)\
- Supported file types\
- Duplicate records

Example error response:

``` json
{
  "code": "BAD_REQUEST",
  "message": "Only CSV files are supported"
}
```

------------------------------------------------------------------------

## Running the application

``` bash
mvn spring-boot:run
```

Swagger UI:

    http://localhost:8080/swagger-ui.html

------------------------------------------------------------------------

## H2 database

The application uses an in-memory **H2 database** for local development.

### Access H2 Console

After starting the application, open:

    http://localhost:8080/h2-console

Use the following settings:

-   **JDBC URL:** `jdbc:h2:mem:bankdb`
-   **User:** `sa`
-   **Password:** *(leave empty)*

------------------------------------------------------------------------

## Tests

``` bash
mvn test
```

Coverage report:

    target/site/jacoco/index.html

------------------------------------------------------------------------

## Author

DaivaGi

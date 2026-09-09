# commerce-order-service

Order management microservice for the Commerce platform.

## Capabilities

- Create orders with one or more line items
- Calculate line totals and order totals
- Generate unique order numbers
- Read orders by ID or order number
- Update order status
- Cancel orders without physical deletion

## API

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `GET /api/orders/number/{orderNumber}`
- `PUT /api/orders/{id}/status`
- `DELETE /api/orders/{id}`

Swagger UI is available locally at:

`http://localhost:8080/swagger-ui/index.html`

## Local Configuration

The service defaults to PostgreSQL at:

`jdbc:postgresql://localhost:5432/order_db`

Configuration can be overridden with:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Run Locally

```bash
./mvnw spring-boot:run
```

Or with PostgreSQL via Docker Compose:

```bash
docker compose up --build
```

## Observability

Actuator endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

Custom metrics:

- `order.created.count`
- `order.status.updated.count`
- `order.cancelled.count`
- `order.lookup.count`
- `order.list.count`

## Future Integrations

This service intentionally does not yet integrate with Product Service, Kafka, Resilience4j, the API Gateway, or security. Those are expected to be added in later steps.

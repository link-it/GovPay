# GovPay - Docker

GovPay può essere facilmente installato via docker su server Linux utilizzando il file Dockerfile presente in questo repository.

I passi da seguire sono i seguenti dalla root del progetto:
- `mvn clean install -DskipTests`
- `docker build . --tag govpay:latest`
- `docker-compose up -d`

L'applicazione sarà disponibile alla url http://localhost:8080/govpay/backend/gui/backoffice le credenzial di accesso i default sono:

- Nome utente: **gpadmin**
- Password: **cambialaosarailicenziato**
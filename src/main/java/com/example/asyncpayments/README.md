# AsyncPayments

AsyncPayments é uma aplicação Java Spring Boot para gerenciamento de pagamentos síncronos e assíncronos, com autenticação JWT, integração com gateways de pagamento e suporte a múltiplos métodos de conexão (INTERNET, BLUETOOTH, SMS, NFC).

## Funcionalidades

- Cadastro e autenticação de usuários com JWT
- Criação automática de contas síncrona e assíncrona para cada usuário
- Realização de transações entre contas (síncronas e assíncronas)
- Transferência entre contas do mesmo usuário (síncrona → assíncrona)
- Listagem de usuários e transações
- Sincronização manual e automática de contas assíncronas
- Bloqueio automático de contas assíncronas inativas
- Integração com gateways de pagamento (STRIPE, PAGARME, MERCADO_PAGO, INTERNO)
- Logging detalhado e tratamento global de exceções

## Endpoints

### Autenticação

- `POST /auth/register`  
  Registra um novo usuário  
  **Body:**  
  ```json
  { "email": "user@email.com", "password": "senha", "role": "USER" }
  ```

- `POST /auth/login`  
  Realiza login e retorna JWT  
  **Body:**  
  ```json
  { "email": "user@email.com", "password": "senha" }
  ```

- `GET /auth/me/id`  
  Retorna o ID do usuário autenticado

- `GET /auth/user/id?email=...`  
  Retorna o ID de um usuário pelo e-mail

### Usuários

- `GET /usuarios/listar`  
  Lista todos os usuários com suas contas

### Transações

- `GET /transacoes/todas`  
  Lista todas as transações

- `POST /transacoes/realizar`  
  Realiza uma transação entre contas  
  **Body:**  
  ```json
  {
    "idUsuarioOrigem": 1,
    "idUsuarioDestino": 2,
    "valor": 50.0,
    "metodoConexao": "INTERNET",
    "gatewayPagamento": "STRIPE"
  }
  ```

- `POST /acoes/adicionar-assincrona/{idUsuario}`  
  Transfere saldo da conta síncrona para a assíncrona do mesmo usuário  
  **Body:**  
  ```json
  {
    "valor": 10.0
  }
  ```

### Sincronização

- `POST /sincronizacao/manual`  
  Sincroniza todas as contas assíncronas manualmente

- `POST /sincronizacao/manual/{id}`  
  Sincroniza uma conta assíncrona específica pelo ID

## Dependências

Veja todas as dependências em [`pom.xml`](pom.xml):

- Java 21
- Spring Boot 3.4.4
- Spring Data JPA
- Spring Security
- JWT (com.auth0:java-jwt)
- Lombok
- H2 Database (testes)
- PostgreSQL (produção)
- Stripe Java SDK
- Jakarta Validation

## Como clonar, instalar e rodar

### 1. Clone o repositório

```sh
git clone https://github.com/seu-usuario/asyncpayments.git
cd asyncpayments
```

### 2. Configure o banco de dados

Edite [`src/main/resources/application.properties`](src/main/resources/application.properties) para ajustar as credenciais do banco de dados PostgreSQL:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/asyncpayments
spring.datasource.username=postgres
spring.datasource.password=passwordtest
```

> Para testes, o H2 Database pode ser usado (já configurado no projeto).

### 3. Instale as dependências

```sh
./mvnw clean install
```
ou
```sh
mvn clean install
```

### 4. Rode a aplicação

```sh
./mvnw spring-boot:run
```
ou
```sh
mvn spring-boot:run
```

A aplicação estará disponível em [http://localhost:8080](http://localhost:8080).

### 5. Teste os endpoints

Utilize Postman, Insomnia ou cURL para testar os endpoints descritos acima.

## Estrutura do Projeto

- Código principal: [`src/main/java/com/example/asyncpayments/AsyncpaymentsApplication.java`](src/main/java/com/example/asyncpayments/AsyncpaymentsApplication.java)
- Configurações: [`src/main/resources/application.properties`](src/main/resources/application.properties)
- Testes: [`src/test/java/com/example/asyncpayments/`](src/test/java/com/example/asyncpayments/)

## Licença

Este projeto está sob a licença MIT.

---

Para dúvidas ou contribuições, abra uma issue ou pull request!
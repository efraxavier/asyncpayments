# AsyncPayments

AsyncPayments é uma aplicação Java Spring Boot para gerenciamento de pagamentos síncronos e assíncronos, com autenticação JWT, integração com gateways de pagamento e suporte a múltiplos métodos de conexão (INTERNET, BLUETOOTH, SMS, NFC, ASYNC).

---

## **Funcionalidades**

- **Autenticação e Cadastro**:
  - Registro de usuários com criação automática de contas síncronas e assíncronas.
  - Autenticação com JWT.
  - Exclusão de usuários autenticados.

- **Transações**:
  - Realização de transações entre contas (síncronas e assíncronas).
  - Transferência entre contas do mesmo usuário (síncrona → assíncrona).
  - Listagem de transações enviadas e recebidas.
  - Sincronização de transações offline.

- **Sincronização de Contas**:
  - Sincronização manual e automática de contas assíncronas.
  - Bloqueio automático de contas assíncronas inativas.

- **Integração com Gateways de Pagamento**:
  - Suporte a múltiplos gateways: STRIPE, PAGARME, MERCADO_PAGO, INTERNO, DREX, PAGSEGURO, PAYCERTIFY.

---

## **Endpoints**

### **Autenticação**

- `POST /auth/register`  
  Registra um novo usuário.  
  **Body:**  
  ```json
  {
    "email": "user@email.com",
    "password": "senha",
    "cpf": "12345678901",
    "nome": "Nome",
    "sobrenome": "Sobrenome",
    "celular": "11999999999",
    "role": "USER",
    "consentimentoDados": true
  }
  ```

- `POST /auth/login`  
  Realiza login e retorna JWT.  
  **Body:**  
  ```json
  {
    "email": "user@email.com",
    "password": "senha"
  }
  ```

- `GET /auth/user/id?email=...`  
  Retorna o ID de um usuário pelo e-mail.

---

### **Usuários**

- `GET /usuarios`  
  Lista todos os usuários com suas contas (somente ADMIN).

- `GET /usuarios/me`  
  Retorna dados e saldos do usuário autenticado.

- `PUT /usuarios/me`  
  Atualiza os dados do usuário autenticado.  
  **Body:**  
  ```json
  {
    "email": "novo@email.com",
    "cpf": "12345678901",
    "nome": "Novo Nome",
    "sobrenome": "Novo Sobrenome",
    "celular": "11999999999"
  }
  ```

- `DELETE /usuarios/me`  
  Exclui o usuário autenticado.

---

### **Transações**

- `POST /transacoes`  
  Realiza uma transação entre contas.  
  **Body:**  
  ```json
  {
    "idUsuarioOrigem": 1,
    "idUsuarioDestino": 2,
    "valor": 50.0,
    "gatewayPagamento": "STRIPE",
    "metodoConexao": "INTERNET",
    "descricao": "Pagamento de serviço"
  }
  ```

- `GET /transacoes/recebidas`  
  Lista transações recebidas pelo usuário autenticado.

- `GET /transacoes/enviadas`  
  Lista transações enviadas pelo usuário autenticado.

- `POST /transacoes/adicionar-fundos`  
  Transfere saldo da conta síncrona para a assíncrona do mesmo usuário.  
  **Body:**  
  ```json
  {
    "idUsuarioOrigem": 20,
    "idUsuarioDestino": 20,
    "valor": 3.0,
    "metodoConexao": "ASYNC",
    "gatewayPagamento": "INTERNO",
    "descricao": "Adição de fundos à conta assíncrona"
  }
  ```

  **Resposta:**  
  ```json
  {
    "id": 57,
    "idUsuarioOrigem": 20,
    "idUsuarioDestino": 20,
    "valor": 3.0,
    "tipoTransacao": "ASSINCRONA",
    "metodoConexao": "ASYNC",
    "gatewayPagamento": "INTERNO",
    "descricao": null,
    "dataCriacao": "2025-06-08T00:22:55.6432361Z",
    "dataAtualizacao": "2025-06-08T00:22:55.6432361Z",
    "sincronizada": false
  }
  ```

---

### **Sincronização**

- `POST /sincronizacao/manual`  
  Sincroniza todas as contas assíncronas manualmente (somente ADMIN).

- `POST /sincronizacao/manual/{id}`  
  Sincroniza uma conta assíncrona específica pelo ID (somente ADMIN).

- `POST /sincronizacao/me`  
  Sincroniza a conta assíncrona do usuário autenticado.

---

## **Configuração**

### **Banco de Dados**

Edite o arquivo [`application.properties`](src/main/resources/application.properties) para configurar o banco de dados PostgreSQL:

```
spring.datasource.url=jdbc:postgresql://localhost:5432/asyncpayments
spring.datasource.username=postgres
spring.datasource.password=passwordtest
```

Para testes, o H2 Database já está configurado.

---

## **Instalação**

1. Clone o repositório:
   ```bash
   git clone https://github.com/seu-usuario/asyncpayments.git
   cd asyncpayments
   ```

2. Instale as dependências:
   ```bash
   ./mvnw clean install
   ```

3. Rode a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

A aplicação estará disponível em [http://localhost:8080](http://localhost:8080).

---

## **Testes**

Execute os testes automatizados com:
```bash
./mvnw test
```

---

## **Estrutura do Projeto**

- **Código principal**: [`src/main/java/com/example/asyncpayments/`](src/main/java/com/example/asyncpayments/)
- **Configurações**: [`src/main/resources/application.properties`](src/main/resources/application.properties)
- **Testes**: [`src/test/java/com/example/asyncpayments/`](src/test/java/com/example/asyncpayments/)

---

## **Licença**

Este projeto está sob a licença MIT.

---

Para dúvidas ou contribuições, abra uma issue ou pull request!

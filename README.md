# AsyncPayments

AsyncPayments é uma aplicação Java Spring Boot para gerenciamento de pagamentos síncronos e assíncronos, com autenticação JWT, integração com gateways de pagamento e suporte a múltiplos métodos de conexão (INTERNET, BLUETOOTH, SMS, NFC, ASYNC).

---

## **Funcionalidades**

- **Autenticação e Cadastro**:
  - Registro de usuários com criação automática de contas síncronas e assíncronas.
  - Autenticação com JWT.
  - Exclusão e atualização de usuários autenticados.

- **Transações**:
  - Realização de transações entre contas (síncronas, assíncronas e internas).
  - Transferência entre contas do mesmo usuário (síncrona → assíncrona) usando tipo de operação `INTERNA`.
  - Listagem de transações enviadas, recebidas e filtradas por qualquer campo.
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
    "tipoOperacao": "SINCRONA",
    "descricao": "Pagamento de serviço"
  }
  ```

- `GET /transacoes`  
  Lista transações com filtros por qualquer campo (exemplo de uso):
  ```
  /transacoes?status=SINCRONIZADA&idUsuarioOrigem=1&dataCriacaoInicio=2025-06-01T00:00:00Z&dataCriacaoFim=2025-06-12T23:59:59Z
  ```
  **Filtros disponíveis:**  
  - id, idUsuarioOrigem, idUsuarioDestino, valor, tipoOperacao, metodoConexao, gatewayPagamento, status, descricao, nomeUsuarioOrigem, emailUsuarioOrigem, cpfUsuarioOrigem, nomeUsuarioDestino, emailUsuarioDestino, cpfUsuarioDestino, dataCriacaoInicio, dataCriacaoFim, dataAtualizacaoInicio, dataAtualizacaoFim

- `GET /transacoes/recebidas`  
  Lista transações recebidas pelo usuário autenticado.

- `GET /transacoes/enviadas`  
  Lista transações enviadas pelo usuário autenticado.

- `POST /transacoes/adicionar-fundos`  
  Transfere saldo da conta síncrona para a assíncrona do mesmo usuário (tipo de operação `INTERNA`).  
  **Body:**  
  ```json
  {
    "valor": 3.0
  }
  ```
  **Resposta:**  
  ```json
  {
    "id": 57,
    "idUsuarioOrigem": 20,
    "idUsuarioDestino": 20,
    "valor": 3.0,
    "tipoOperacao": "INTERNA",
    "metodoConexao": "ASYNC",
    "gatewayPagamento": "INTERNO",
    "descricao": "Adição de fundos da conta síncrona para assincrona",
    "dataCriacao": "2025-06-08T00:22:55.6432361Z",
    "dataAtualizacao": "2025-06-08T00:22:55.6432361Z",
    "sincronizada": false
  }
  ```

- `GET /transacoes/{id}`  
  Busca uma transação por ID.

- `GET /transacoes/{id}/status`  
  Consulta o status da transação.

- `PUT /transacoes/{id}/status?novoStatus=SINCRONIZADA`  
  Atualiza o status da transação (ADMIN).

---

### **Sincronização**

- `POST /sincronizacao/manual`  
  Sincroniza todas as contas assíncronas manualmente (somente ADMIN).

- `POST /sincronizacao/manual/{id}`  
  Sincroniza uma conta assíncrona específica pelo ID (somente ADMIN).

- `POST /sincronizacao/me`  
  Sincroniza a conta assíncrona do usuário autenticado.

---

### **Logs**

- `GET /api/logs`  
  (ADMIN) Visualiza todos os logs do sistema, incluindo logs de API, backend e frontend.

---

- **Sincronização:**  
  - Só pode ser marcada como `SINCRONIZADA` se o saldo da conta assíncrona for igual ao valor enviado e a operação ocorrer dentro de 72h.
  - Após sincronizar, o saldo da conta assíncrona é zerado e o valor transferido para a conta síncrona.
  - Se não atender as condições, a transação é marcada como `ROLLBACK` e os saldos não são alterados.

- **Transações offline:**  
  - Só podem ser processadas até 72h após a criação.
  - Limite de R$500,00 por transação offline.

- **Limite diário:**  
  - Máximo de R$1000,00 por usuário/dia para transações síncronas.

- **Transações acima de R$10.000,00:**  
  - Notificam o BACEN e são registradas em tabela especial.

- **Rollback automático:**  
  - Transações pendentes há mais de 72h são revertidas e o saldo devolvido.

- **Logs:**  
  - Todas as respostas de API relevantes são logadas e podem ser consultadas via endpoint.

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
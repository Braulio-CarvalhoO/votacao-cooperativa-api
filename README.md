# Votação Cooperativa — API REST

API REST desenvolvida em Java 17 e Spring Boot 3 para gerenciamento de pautas e sessões de votação em uma cooperativa.

O projeto foi desenvolvido como solução para um desafio técnico de Backend Java, contemplando cadastro de pautas, abertura de sessões de votação, registro de votos, controle de voto único por associado e apuração dos resultados.

---

## 📌 Funcionalidades

A API permite:
* Cadastro e consulta de pautas;
* Abertura de sessões de votação com duração configurável (duração padrão de 1 minuto quando não informada);
* Registro de votos (`SIM` ou `NAO`);
* Controle rigoroso de voto único por associado em cada pauta (com tratativa de concorrência);
* Apuração e resultado da votação;
* Persistência dos dados em banco de dados;
* Validação de aptidão de voto do associado através de um simulador de integração (Mock determinístico);
* Documentação interativa da API através do Swagger/OpenAPI;
* Cobertura de testes automatizados.

---

## 🏗️ Arquitetura

O projeto utiliza uma arquitetura em camadas simples, coesa e focada na separação de responsabilidades:

```text
Controller  →  Service  →  Repository  →  Database
```

* **Controller**: Recebe as requisições HTTP, valida entradas e expõe os contratos REST.
* **Service**: Centraliza as regras de negócio (abertura e ciclo de vida da sessão, controle de duplicidade de voto, validação do associado e apuração dos resultados).
* **Repository**: Interface de persistência com o banco de dados via Spring Data JPA.
* **DTO**: Representação dos contratos de entrada e saída da API.
* **Model**: Entidades de domínio e persistência JPA.
* **Exception**: Mapeamento e centralização de exceções de negócio (`@RestControllerAdvice`).
* **Client**: Componente encarregado da validação de aptidão do associado (Simulador de Integração/Mock).
* **Config**: Propriedades customizadas e configurações da aplicação.

---

## 🛠️ Tecnologias

* **Java 17**
* **Spring Boot 3** (Spring Web, Spring Data JPA)
* **H2 Database** (modo arquivo)
* **Maven**
* **JUnit 5, Mockito & MockMvc**
* **OpenAPI / Swagger**

---

## 📂 Estrutura do Projeto

```text
src
├── main
│   ├── java
│   │   └── com.cooperativa.votacao
│   │       ├── client
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── enums
│   │       ├── exception
│   │       ├── model
│   │       ├── repository
│   │       └── service
│   └── resources
│       └── application.yml
└── test
    └── java
        └── com.cooperativa.votacao
            ├── controller
            └── service
```

---

## 🚀 Como Executar

### Pré-requisitos
* JDK 17 ou superior
* Maven 3.9 ou superior

### Executando a Aplicação
Na raiz do projeto, execute:

```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.

---

## 🗄️ Banco de Dados

O projeto utiliza o **H2 Database** em modo arquivo para garantir a persistência dos dados entre reinicializações sem depender de um banco relacional externo na máquina de quem executa.

* **JDBC URL:** `jdbc:h2:file:./data/votacaodb`
* **Diretório de dados:** `./data`
* **H2 Console:** `http://localhost:8080/h2-console`
    * **User:** `sa`
    * **Password:** *(em branco)*

---

## 📖 Swagger / OpenAPI

A documentação interativa da API está disponível via Swagger UI em:

```text
http://localhost:8080/swagger-ui.html
```

---

## 📡 Endpoints da API

A API é versionada pelo prefixo `/api/v1`.

### 1. Pautas

#### Cadastrar Pauta
```http
POST /api/v1/pautas
Content-Type: application/json

{
  "titulo": "Aprovar novo estatuto social",
  "descricao": "Alterações nas cláusulas de participação nas assembleias"
}
```
* **Retorno:** `201 Created`

#### Listar Pautas
```http
GET /api/v1/pautas
```

#### Consultar Pauta por ID
```http
GET /api/v1/pautas/1
```

---

### 2. Sessão de Votação

#### Abrir Sessão de Votação
```http
POST /api/v1/pautas/{pautaId}/sessoes
Content-Type: application/json

{
  "duracaoSegundos": 120
}
```
*(Se `duracaoSegundos` não for enviado, o tempo padrão de **60 segundos** é aplicado).*

---

### 3. Registro de Votos

#### Registrar Voto
```http
POST /api/v1/pautas/{pautaId}/votos
Content-Type: application/json

{
  "associadoId": "12345678900",
  "voto": "SIM"
}
```
* **Valores aceitos para voto:** `SIM`, `NAO`

---

### 4. Apuração e Resultado

#### Consultar Resultado
```http
GET /api/v1/pautas/{pautaId}/resultado
```

Exemplo de resposta:
```json
{
  "pautaId": 1,
  "titulo": "Aprovar novo estatuto social",
  "sessaoEncerrada": true,
  "totalVotosSim": 7,
  "totalVotosNao": 3,
  "totalVotos": 10,
  "resultado": "APROVADA"
}
```
* **Status do Resultado:** `VOTACAO_EM_ANDAMENTO`, `APROVADA`, `REJEITADA`, `EMPATE`.

---

## 🔒 Regras de Negócio & Resiliência

### 1. Voto Único por Associado
Um associado só pode votar uma única vez por pauta.
* **Camada de Aplicação:** A camada de serviço verifica a existência do voto antes de salvar.
* **Camada de Banco de Dados:** Constraint Única composta por `(sessao_id, associado_id)`. Se duas requisições simultâneas contornarem a validação do serviço, a violação de integridade do banco é capturada e tratada com uma `NegocioException`, garantindo consistência em cenários de alta concorrência.

### 2. Encerramento de Sessão sem Scheduler
A sessão é calculada dinamicamente comparando `LocalDateTime.now()` com o campo `dataFechamento`. Essa decisão simplifica a arquitetura e elimina a necessidade de jobs assíncronos em background ou schedulers de estado.

---

## 🔐 Validação do Associado (Integração & Mock)

Como requisito do desafio, foi implementada a verificação de aptidão de voto do associado (`ABLE_TO_VOTE` / `UNABLE_TO_VOTE`).

### Decisão Técnica de Arquitetura (Simulador / Mock)
O endpoint externo original fornecido no desafio (`https://user-info.herokuapp.com/users/{cpf}`) encontra-se **permanentemente descontinuado/fora do ar** devido ao encerramento dos planos gratuitos da plataforma Heroku.

Para evitar falhas de rede, lentidão por timeouts ou dependência de uma infraestrutura externa inoperante, o componente `AssociadoValidadorClient` foi projetado como um **Simulador Local Determinístico (Mock)**:

* **Validação Determinística por CPF:**
    * **Dígito final PAR** (ex: `12345678900`): Simula associado apto a votar (`ABLE_TO_VOTE`).
    * **Dígito final ÍMPAR (1, 3, 5, 7)** (ex: `12345678901`): Simula associado inapto (`UNABLE_TO_VOTE`) → Retorna `AssociadoNaoAptoException`.
    * **Dígito final 9** (ex: `12345678909`): Simula CPF não encontrado ou inválido na base externa.

Essa estratégia garante que **100% dos cenários de teste e validação da API funcionem de ponta a ponta**, demonstrando tanto o fluxo de aprovação quanto o de bloqueio de voto sem requisições HTTP quebradas.

---

## 🎁 Tarefas Bônus Atendidas

* **Bônus 1 — Validação do Associado:** Implementada via `AssociadoValidadorClient` com simulação determinística completa de cenários de aptidão e rejeição.
* **Bônus 2 — Performance na Apuração:** A contagem de votos é realizada diretamente no banco de dados via consulta de agregação (`COUNT / GROUP BY`), evitando carregar listas de objetos em memória.
* **Bônus 3 — Versionamento de API:** URL devidamente versionada com o prefixo `/api/v1/`.

---

## 🧪 Testes Automatizados

O projeto conta com testes unitários e de integração utilizando **JUnit 5**, **Mockito** e **MockMvc**, cobrindo:
* Abertura de sessões e duração padrão/customizada;
* Bloqueio de votos em sessões encerradas ou com associados duplicados;
* Apuração de resultados e empates;
* Comportamento e exceções da validação do associado;
* Controladores REST e respostas HTTP.

Para executar a suíte de testes:

```bash
mvn test
```

---

## 🧠 Tratamento de Erros

A API utiliza `@RestControllerAdvice` na classe `ApiExceptionHandler` para padronizar as respostas de erro da aplicação:

* `400 Bad Request`: Erros de validação de payload ou regras de negócio (ex: voto duplicado, sessão fechada).
* `403 Forbidden`: Associado não apto a votar (`AssociadoNaoAptoException`).
* `404 Not Found`: Pauta ou Sessão não encontrada (`RecursoNaoEncontradoException`).
* `500 Internal Server Error`: Erros não esperados do sistema.

---

## 👨‍💻 Autor

**Bráulio Carvalho**  
Projeto desenvolvido como solução para o desafio técnico de Backend Java.
# 🏦 Nimble Gateway

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-51%20passing-brightgreen.svg)](#-testes)
[![Coverage](https://img.shields.io/badge/Coverage-11%25-red.svg)](#-cobertura-de-código)

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Uso](#-uso)
- [API Endpoints](#-api-endpoints)
- [Testes](#-testes)
- [Cobertura de Código](#-cobertura-de-código)
- [Docker](#-docker)
- [Contribuição](#-contribuição)
- [Licença](#-licença)

## 🎯 Sobre o Projeto

O **Nimble Gateway** é um gateway de pagamentos simplificado desenvolvido como parte do desafio técnico para a posição de **Desenvolvedor Backend Pleno** na **Nimble Soluções em Banking e Meios de Pagamento**.

### Objetivo

Desenvolver uma aplicação que funcione como um gateway de pagamentos simplificado, permitindo que usuários realizem operações como:

- ✅ **Cadastro e autenticação** de usuários
- ✅ **Criação e gestão** de cobranças
- ✅ **Pagamentos** entre contas (saldo e cartão)
- ✅ **Depósitos** com autorização externa
- ✅ **Cancelamento** de cobranças

### Avaliação Técnica

Este projeto demonstra habilidades em:

- 🏗️ **Design de Software** - Estruturação e organização do código
- 🌐 **APIs RESTful** - Endpoints seguindo padrões REST
- 🗄️ **Banco de Dados Relacional** - Modelagem e interação eficiente
- 🔗 **Integração Externa** - Chamadas a serviços externos
- 🔒 **Segurança** - Autenticação JWT e hash de senhas
- 🧪 **Testes** - Cobertura de código e qualidade
- 🐳 **Containerização** - Docker para facilitar deploy

## 🚀 Funcionalidades

### 1. 👤 Gerenciamento de Usuários

- **Cadastro de Usuário**
  - Campos obrigatórios: Nome, CPF, e-mail e senha
  - Validação de CPF com algoritmo próprio
  - Senha armazenada com hash seguro (BCrypt)
  - Validação de dados com Bean Validation

- **Autenticação**
  - Login por CPF ou e-mail
  - Autenticação JWT com refresh token
  - Proteção de endpoints sensíveis
  - Rate limiting para segurança

### 2. 💰 Gestão de Cobranças

- **Criação de Cobranças**
  - Originador cria cobrança para destinatário
  - Busca por CPF do destinatário
  - Campos: CPF, valor, descrição opcional
  - Validação de usuários existentes

- **Consulta de Cobranças**
  - Cobranças enviadas (por originador)
  - Cobranças recebidas (por destinatário)
  - Filtros por status: Pendente, Paga, Cancelada
  - Paginação e ordenação

### 3. 💳 Pagamentos

- **Pagamento por Saldo**
  - Verificação de saldo suficiente
  - Débito do pagador e crédito do destinatário
  - Transações atômicas com rollback

- **Pagamento por Cartão**
  - Integração com autorizador externo
  - Campos: número, data de expiração, CVV
  - Validação de dados do cartão

- **Depósito de Saldo**
  - Autorização externa obrigatória
  - Adição segura ao saldo do usuário
  - Auditoria de transações

### 4. ❌ Cancelamento

- **Cobranças Pendentes**
  - Mudança de status para cancelada
  - Apenas o originador pode cancelar

- **Cobranças Pagas**
  - Estorno automático para pagamentos com saldo
  - Autorização externa para cartão de crédito
  - Reversão de transações

## 🛠️ Tecnologias

### Backend
- **Java 25** - Linguagem principal
- **Spring Boot 3.4.10** - Framework
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **Hibernate** - ORM
- **Maven** - Gerenciamento de dependências

### Banco de Dados
- **H2** - Banco em memória para testes
- **PostgreSQL** - Banco de produção
- **Flyway** - Migração de banco

### Testes
- **JUnit 5** - Framework de testes
- **Mockito** - Mocking
- **TestRestTemplate** - Testes de integração
- **JaCoCo** - Cobertura de código

### Documentação
- **Swagger/OpenAPI** - Documentação da API
- **SpringDoc** - Geração automática

### Outras
- **Docker** - Containerização
- **Lombok** - Redução de boilerplate
- **MapStruct** - Mapeamento de DTOs
- **Resilience4j** - Circuit breaker

## 🏗️ Arquitetura

### Padrão Arquitetural
O projeto segue os princípios da **Clean Architecture** com separação clara de responsabilidades:

```
src/main/java/com/nimble/gateway/
├── application/          # Casos de uso e DTOs
│   ├── dto/            # Data Transfer Objects
│   ├── mapper/         # Mapeadores
│   ├── usecase/        # Lógica de negócio
│   └── validation/     # Validações customizadas
├── domain/             # Entidades e regras de negócio
│   ├── entity/         # Entidades JPA
│   ├── exception/      # Exceções de domínio
│   ├── repository/     # Interfaces de repositório
│   └── valueobject/    # Value Objects
├── infrastructure/     # Implementações técnicas
│   ├── audit/          # Auditoria
│   ├── config/         # Configurações
│   ├── external/       # Integrações externas
│   ├── health/         # Health checks
│   ├── metrics/        # Métricas
│   └── security/       # Segurança
└── presentation/       # Controllers e exceções
    ├── controller/     # REST Controllers
    └── exception/      # Tratamento de exceções
```

### Fluxo de Dados
```
Controller → UseCase → Repository → Database
     ↓         ↓
   DTO ←→ Entity
```

## 📦 Instalação

### Pré-requisitos
- **Java 25+**
- **Maven 3.8+**
- **Docker** (opcional)
- **Git**

### 1. Clone o Repositório
```bash
git clone https://github.com/seu-usuario/nimble-gateway.git
cd nimble-gateway
```

### 2. Configuração do Ambiente
```bash
# Verificar versão do Java
java -version

# Verificar versão do Maven
mvn -version
```

### 3. Executar a Aplicação

#### Opção 1: Maven
```bash
# Compilar o projeto
./mvnw clean compile

# Executar testes
./mvnw test

# Executar a aplicação
./mvnw spring-boot:run
```

#### Opção 2: Docker
```bash
# Construir a imagem
docker build -t nimble-gateway .

# Executar o container
docker run -p 8080:8080 nimble-gateway
```

## ⚙️ Configuração

### Arquivo de Configuração
```yaml
# application.yml
spring:
  application:
    name: nimble-gateway
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/nimble_gateway
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key}
      expiration: 86400000

# Autorizador externo
external:
  authorizer:
    url: https://zsy6tx7aql.execute-api.sa-east-1.amazonaws.com/authorizer
    timeout: 5000
```

### Variáveis de Ambiente
```bash
# Banco de dados
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_URL=jdbc:postgresql://localhost:5432/nimble_gateway

# JWT
export JWT_SECRET=your-super-secret-key

# Perfil
export SPRING_PROFILES_ACTIVE=prod
```

## 🚀 Uso

### 1. Iniciar a Aplicação
```bash
./mvnw spring-boot:run
```

### 2. Acessar a Documentação
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### 3. Exemplo de Uso

#### Cadastro de Usuário
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "cpf": "12345678901",
    "email": "joao@example.com",
    "password": "12345678"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao@example.com",
    "password": "12345678"
  }'
```

## 📚 API Endpoints

### 🔐 Autenticação
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/auth/register` | Cadastro de usuário |
| `POST` | `/api/auth/login` | Login de usuário |
| `GET` | `/api/auth/me` | Dados do usuário atual |

### 💰 Cobranças
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/charges` | Criar cobrança |
| `GET` | `/api/charges/sent` | Cobranças enviadas |
| `GET` | `/api/charges/received` | Cobranças recebidas |
| `GET` | `/api/charges/{id}` | Cobrança específica |

### 💳 Pagamentos
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/payments/pay` | Pagar cobrança |
| `POST` | `/api/payments/deposit` | Fazer depósito |
| `POST` | `/api/payments/cancel/{id}` | Cancelar cobrança |

### 📊 Monitoramento
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/actuator/health` | Status da aplicação |
| `GET` | `/actuator/metrics` | Métricas da aplicação |

## 🧪 Testes

### Executar Testes
```bash
# Todos os testes
./mvnw test

# Testes com cobertura
./mvnw test jacoco:report

# Testes específicos
./mvnw test -Dtest=UserUseCaseTest
```

### Estrutura de Testes
```
src/test/java/
├── com/nimble/gateway/
│   ├── application/usecase/    # Testes unitários
│   ├── domain/valueobject/     # Testes de value objects
│   ├── integration/           # Testes de integração
│   └── StartupTests.java      # Testes de startup
```

### Métricas de Testes
- **Total de Testes**: 51
- **Taxa de Sucesso**: 100%
- **Testes Unitários**: 42
- **Testes de Integração**: 9

## 📊 Cobertura de Código

### Relatório JaCoCo
```bash
# Gerar relatório de cobertura
./mvnw test jacoco:report

# Visualizar relatório
open target/site/jacoco/index.html
```

### Métricas Atuais
- **Instruções**: 11% (3,612/4,062)
- **Branches**: 0% (246/246)
- **Linhas**: 8% (913/998)
- **Métodos**: 12% (184/209)
- **Classes**: 33% (29/43)

### Pacotes com Melhor Cobertura
- `infrastructure.config`: **69%**
- `domain.entity`: **36%**
- `infrastructure.security`: **26%**

## 🐳 Docker

### Dockerfile
```dockerfile
FROM openjdk:25-jdk-slim

WORKDIR /app

COPY target/nimble-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - postgres

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: nimble_gateway
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### Comandos Docker
```bash
# Construir e executar
docker-compose up --build

# Executar em background
docker-compose up -d

# Parar serviços
docker-compose down
```

## 🔧 Desenvolvimento

### Estrutura do Projeto
```
nimble-gateway/
├── src/
│   ├── main/java/          # Código fonte
│   ├── main/resources/     # Configurações
│   ├── test/java/          # Testes
│   └── test/resources/     # Configurações de teste
├── target/                 # Build output
├── docker-compose.yml      # Docker Compose
├── Dockerfile             # Docker image
├── pom.xml                # Maven configuration
└── README.md              # Este arquivo
```

### Padrões de Código
- **Clean Code** - Código limpo e legível
- **SOLID** - Princípios de design
- **DRY** - Don't Repeat Yourself
- **KISS** - Keep It Simple, Stupid

### Commits
```bash
# Padrão de commits
feat: adicionar nova funcionalidade
fix: corrigir bug
docs: atualizar documentação
test: adicionar testes
refactor: refatorar código
```

## 🤝 Contribuição

### Como Contribuir
1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

### Padrões de Desenvolvimento
- Seguir as convenções de código Java
- Escrever testes para novas funcionalidades
- Manter a cobertura de código
- Documentar mudanças significativas

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 👨‍💻 Autor

**Desenvolvedor Backend Pleno**  
*Desafio Técnico - Nimble Soluções*

- **Contato**: +55 15 99652-0886
- **Empresa**: Nimble Soluções em Banking e Meios de Pagamento LTDA
- **CNPJ**: 48.132.833/0001-32
- **Endereço**: Alameda Rio Negro, nº 503, sala 2020, Alphaville Centro Empresarial, Barueri/SP, CEP 06454-000

---

<div align="center">

**Desenvolvido com ❤️ para o Desafio Técnico da Nimble**

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-blue.svg)](https://maven.apache.org/)

</div>

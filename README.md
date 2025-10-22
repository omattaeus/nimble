# 🏦 Nimble Gateway

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9.11-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-18%20classes-brightgreen.svg)](#-testes)
[![Coverage](https://img.shields.io/badge/Coverage-JaCoCo%20Ready-blue.svg)](#-cobertura-de-código)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](#-docker)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-Circuit%20Breaker-green.svg)](#-resiliência-e-monitoramento)

## Índice

- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Uso](#-uso)
- [API Endpoints](#-api-endpoints)
- [Resiliência e Monitoramento](#-resiliência-e-monitoramento)
- [Testes](#-testes)
- [Cobertura de Código](#-cobertura-de-código)
- [Docker](#-docker)
- [Desenvolvimento](#-desenvolvimento)
- [Contribuição](#-contribuição)
- [Licença](#-licença)

## Sobre o Projeto

O **Nimble Gateway** é um gateway de pagamentos simplificado desenvolvido como parte do desafio técnico para a posição de **Desenvolvedor Backend Pleno** na **Nimble Soluções em Banking e Meios de Pagamento**.

### Objetivo

Desenvolver uma aplicação que funcione como um gateway de pagamentos simplificado, permitindo que usuários realizem operações como:

- ✅ **Cadastro e autenticação** de usuários com validação de CPF
- ✅ **Criação e gestão** de cobranças entre usuários
- ✅ **Pagamentos** entre contas (saldo e cartão de crédito)
- ✅ **Depósitos** com autorização externa obrigatória
- ✅ **Cancelamento** de cobranças com estorno automático
- ✅ **Auditoria** completa de todas as transações

### Avaliação Técnica

Este projeto demonstra habilidades avançadas em:

- **Design de Software** - Clean Architecture com separação clara de responsabilidades
- **APIs RESTful** - Endpoints seguindo padrões REST com documentação OpenAPI
- **Banco de Dados Relacional** - Modelagem eficiente com MySQL e Flyway
- **Integração Externa** - Chamadas resilientes a serviços externos
- **Segurança** - Autenticação JWT, hash de senhas e rate limiting
- **Testes** - Cobertura abrangente com testes unitários e de integração
- **Containerização** - Docker para facilitar deploy e desenvolvimento
- **Resiliência** - Circuit breaker, retry e fallbacks para serviços externos

## 🚀 Funcionalidades

### 1. Gerenciamento de Usuários

#### **Cadastro de Usuário**
- **Campos obrigatórios**: Nome, CPF, e-mail e senha
- **Validação de CPF**: Algoritmo próprio com validação matemática
- **Segurança**: Senha armazenada com hash BCrypt
- **Validação**: Bean Validation com mensagens customizadas
- **Unicidade**: CPF e e-mail únicos no sistema

#### **Autenticação**
- **Login flexível**: Por CPF ou e-mail
- **JWT**: Autenticação com access token e refresh token
- **Proteção**: Endpoints sensíveis protegidos
- **Rate Limiting**: Proteção contra ataques de força bruta
- **Sessão**: Gerenciamento seguro de sessões

### 2. Gestão de Cobranças

#### **Criação de Cobranças**
- **Originador**: Usuário que cria a cobrança
- **Destinatário**: Busca por CPF do destinatário
- **Campos**: CPF, valor, descrição opcional
- **Validação**: Usuários existentes e valores positivos
- **Status**: Controle de estados (Pendente, Paga, Cancelada)

#### **Consulta de Cobranças**
- **Enviadas**: Cobranças criadas pelo usuário
- **Recebidas**: Cobranças direcionadas ao usuário
- **Filtros**: Por status (Pendente, Paga, Cancelada)
- **Paginação**: Controle de volume de dados
- **Ordenação**: Por data de criação

### 3. Pagamentos

#### **Pagamento por Saldo**
- **Verificação**: Saldo suficiente do pagador
- **Transação**: Débito do pagador e crédito do destinatário
- **Atomicidade**: Transações com rollback automático
- **Auditoria**: Log completo de todas as operações

#### **Pagamento por Cartão**
- **Integração**: Autorizador externo obrigatório
- **Campos**: Número, data de expiração, CVV
- **Validação**: Dados do cartão e autorização
- **Segurança**: Dados sensíveis não armazenados

#### **Depósito de Saldo**
- **Autorização**: Externa obrigatória para todos os depósitos
- **Segurança**: Adição controlada ao saldo
- **Auditoria**: Rastreamento completo de depósitos

### 4. Cancelamento

#### **Cobranças Pendentes**
- **Simplicidade**: Mudança de status para cancelada
- **Autorização**: Apenas o originador pode cancelar
- **Auditoria**: Log da operação de cancelamento

#### **Cobranças Pagas**
- **Estorno Automático**: Para pagamentos com saldo
- **Autorização Externa**: Para cartão de crédito
- **Reversão**: Transações completamente revertidas
- **Integridade**: Manutenção da consistência dos dados

## Tecnologias

### **Backend Core**
- **Java 21** - Linguagem principal com recursos modernos
- **Spring Boot 3.4.10** - Framework principal
- **Spring Security 6** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **Hibernate 6** - ORM com recursos avançados
- **Maven 3.9.11** - Gerenciamento de dependências

### **Banco de Dados**
- **MySQL 8.4** - Banco de dados principal
- **Flyway** - Migração de banco versionada
- **H2** - Banco em memória para testes

### **Segurança e Autenticação**
- **JWT (JJWT 0.12.3)** - Tokens de autenticação
- **BCrypt** - Hash de senhas
- **Rate Limiting** - Proteção contra ataques

### **Integração e Resiliência**
- **WebFlux** - Cliente HTTP reativo
- **Resilience4j 2.1.0** - Circuit breaker e retry
- **WebClient** - Cliente HTTP não-bloqueante

### **Testes**
- **JUnit 5** - Framework de testes
- **Mockito** - Mocking avançado
- **TestRestTemplate** - Testes de integração
- **TestContainers** - Testes com containers
- **JaCoCo** - Cobertura de código

### **Documentação e Monitoramento**
- **SpringDoc OpenAPI 2.8.13** - Documentação da API
- **Spring Actuator** - Monitoramento e métricas
- **Logback** - Logging estruturado
- **Logstash Encoder** - Logs em JSON

### **Utilitários**
- **Lombok** - Redução de boilerplate
- **MapStruct 1.5.5** - Mapeamento de DTOs
- **Bean Validation** - Validação de dados

## 🏗️ Arquitetura

### **Padrão Arquitetural**
O projeto segue os princípios da **Clean Architecture** com separação clara de responsabilidades:

```
src/main/java/com/nimble/gateway/
├── application/          # Camada de Aplicação
│   ├── dto/            # Data Transfer Objects
│   ├── mapper/         # Mapeadores MapStruct
│   ├── usecase/        # Casos de uso (lógica de negócio)
│   └── validation/     # Validações customizadas
├── domain/             # Camada de Domínio
│   ├── entity/         # Entidades JPA
│   ├── exception/      # Exceções de domínio
│   ├── repository/     # Interfaces de repositório
│   └── valueobject/    # Value Objects (CPF, Money)
├── infrastructure/     # Camada de Infraestrutura
│   ├── audit/          # Serviços de auditoria
│   ├── config/         # Configurações Spring
│   ├── external/       # Integrações externas
│   ├── health/         # Health checks
│   ├── metrics/        # Métricas customizadas
│   ├── persistence/    # Implementações de repositório
│   └── security/       # Configurações de segurança
└── presentation/       # Camada de Apresentação
    ├── controller/     # REST Controllers
    └── exception/      # Tratamento global de exceções
```

### **Fluxo de Dados**
```
Controller → UseCase → Repository → Database
     ↓         ↓
   DTO ←→ Entity (via Mapper)
```

### **Princípios Aplicados**
- **SOLID** - Princípios de design orientado a objetos
- **DRY** - Don't Repeat Yourself
- **KISS** - Keep It Simple, Stupid
- **Clean Code** - Código limpo e legível
- **Domain-Driven Design** - Modelagem baseada no domínio

## 🚀 Instalação

### **Pré-requisitos**
- **Java 21+**
- **Maven 3.8+**
- **Docker** (opcional)
- **Git**
- **MySQL 8.0+** (ou Docker)

### **1. Clone o Repositório**
```bash
git clone https://github.com/seu-usuario/nimble-gateway.git
cd nimble-gateway
```

### **2. Configuração do Ambiente**
```bash
# Verificar versão do Java
java -version

# Verificar versão do Maven
./mvnw -version
```

### **3. Executar a Aplicação**

#### **Opção 1: Maven (Recomendado)**
```bash
# Compilar o projeto
./mvnw clean compile

# Executar testes
./mvnw test

# Executar a aplicação
./mvnw spring-boot:run
```

#### **Opção 2: Docker**
```bash
# Construir a imagem
docker build -t nimble-gateway .

# Executar o container
docker run -p 8080:8080 nimble-gateway
```

#### **Opção 3: Docker Compose (Completo)**
```bash
# Executar com banco de dados
docker-compose up --build
```

## Configuração

### **Arquivo de Configuração Principal**
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/nimble?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root123
  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        jdbc.time_zone: UTC
  flyway:
    enabled: true

# JWT Configuration
app:
  jwt:
    secret: mySecretKey123456789012345678901234567890123456789012345678901234567890
    expiration: 86400000 # 24 hours
    refresh-expiration: 604800000 # 7 days

# External Services
external:
  authorizer:
    url: https://zsy6tx7aql.execute-api.sa-east-1.amazonaws.com

# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    instances:
      authorizerService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
  retry:
    instances:
      authorizerService:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
```

### **Variáveis de Ambiente**
```bash
# Banco de dados
export DB_USERNAME=root
export DB_PASSWORD=root123
export DB_URL=jdbc:mysql://localhost:3307/nimble

# JWT
export JWT_SECRET=your-super-secret-key

# Perfil
export SPRING_PROFILES_ACTIVE=prod
```

## Uso

### **1. Iniciar a Aplicação**
```bash
./mvnw spring-boot:run
```

### **2. Acessar a Documentação**
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Circuit Breaker**: http://localhost:8080/api/health/circuit-breaker

### **3. Exemplo de Uso Completo**

#### **Cadastro de Usuário**
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

#### **Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao@example.com",
    "password": "12345678"
  }'
```

#### **Criar Cobrança**
```bash
curl -X POST http://localhost:8080/api/charges \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "recipientCpf": "98765432100",
    "amount": 100.00,
    "description": "Pagamento de serviços"
  }'
```

#### **Pagar Cobrança**
```bash
curl -X POST http://localhost:8080/api/payments/pay \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "chargeId": "charge-uuid-here",
    "method": "BALANCE"
  }'
```

## 🔗 API Endpoints

### **Autenticação**
| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|--------------|
| `POST` | `/api/auth/register` | Cadastro de usuário | ❌ |
| `POST` | `/api/auth/login` | Login de usuário | ❌ |
| `GET` | `/api/auth/me` | Dados do usuário atual | ✅ |

### **Cobranças**
| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|--------------|
| `POST` | `/api/charges` | Criar cobrança | ✅ |
| `GET` | `/api/charges/sent` | Cobranças enviadas | ✅ |
| `GET` | `/api/charges/received` | Cobranças recebidas | ✅ |
| `GET` | `/api/charges/{id}` | Cobrança específica | ✅ |

### **Pagamentos**
| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|--------------|
| `POST` | `/api/payments/pay` | Pagar cobrança | ✅ |
| `POST` | `/api/payments/deposit` | Fazer depósito | ✅ |
| `POST` | `/api/payments/cancel/{id}` | Cancelar cobrança | ✅ |

### **Monitoramento**
| Método | Endpoint | Descrição | Autenticação |
|--------|----------|-----------|--------------|
| `GET` | `/actuator/health` | Status da aplicação | ❌ |
| `GET` | `/actuator/metrics` | Métricas da aplicação | ❌ |
| `GET` | `/api/health/authorizer` | Status do autorizador | ❌ |
| `GET` | `/api/health/circuit-breaker` | Status do circuit breaker | ❌ |

## 🛡️ Resiliência e Monitoramento

### **Circuit Breaker**
O sistema implementa circuit breaker para o serviço de autorização externa:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authorizerService:
        failure-rate-threshold: 50        # Ativa após 50% de falhas
        wait-duration-in-open-state: 30s  # Espera 30s antes de tentar novamente
        sliding-window-size: 10           # Janela de 10 chamadas
        minimum-number-of-calls: 5        # Mínimo 5 chamadas para ativar
```

### **Retry com Backoff Exponencial**
```yaml
retry:
  instances:
    authorizerService:
      max-attempts: 3                     # Máximo 3 tentativas
      wait-duration: 1s                   # Intervalo de 1 segundo
      exponential-backoff-multiplier: 2   # Backoff exponencial
```

### **Fallback Strategy**
Quando o circuit breaker ativa, o sistema:
1. **Retorna `false`** para autorizações
2. **Loga o evento** para monitoramento
3. **Mantém a aplicação funcionando** sem travar

### **Monitoramento**
- **Health Checks**: Status do autorizador externo
- **Métricas**: Taxa de falhas, número de chamadas
- **Logs**: Estruturados em JSON para análise
- **Actuator**: Endpoints de monitoramento Spring Boot

## Testes

### **Executar Testes**
```bash
# Todos os testes
./mvnw test

# Testes com cobertura
./mvnw test jacoco:report

# Testes específicos
./mvnw test -Dtest=UserUseCaseTest

# Testes de integração
./mvnw test -Dtest=*IntegrationTest
```

### **Estrutura de Testes**
```
src/test/java/
├── com/nimble/gateway/
│   ├── application/usecase/    # Testes unitários de casos de uso
│   ├── domain/valueobject/     # Testes de value objects
│   ├── integration/           # Testes de integração
│   └── StartupTests.java      # Testes de startup
```

### **Métricas de Testes**
- **Total de Classes de Teste**: 18
- **Testes Unitários**: Casos de uso, value objects, validações
- **Testes de Integração**: Controllers, repositórios, serviços externos
- **Cobertura**: JaCoCo configurado para relatórios detalhados

### **Tipos de Teste**
- **Unitários**: Lógica de negócio isolada
- **Integração**: Fluxos completos end-to-end
- **Contrato**: Integração com serviços externos
- **Performance**: Testes de carga e stress

## 📊 Cobertura de Código

### **Relatório JaCoCo**
```bash
# Gerar relatório de cobertura
./mvnw test jacoco:report

# Visualizar relatório
open target/site/jacoco/index.html
```

### **Configuração JaCoCo**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### **Métricas de Qualidade**
- **Instruções**: Cobertura de linhas executadas
- **Branches**: Cobertura de ramificações condicionais
- **Linhas**: Cobertura de linhas de código
- **Métodos**: Cobertura de métodos
- **Classes**: Cobertura de classes

## 🐳 Docker

### **Dockerfile**
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/Nimble-Gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **Docker Compose**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_URL=jdbc:mysql://mysql:3306/nimble
    depends_on:
      - mysql

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: nimble
      MYSQL_USER: root
      MYSQL_PASSWORD: root123
      MYSQL_ROOT_PASSWORD: root123
    ports:
      - "3307:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:
```

### **Comandos Docker**
```bash
# Construir e executar
docker-compose up --build

# Executar em background
docker-compose up -d

# Parar serviços
docker-compose down

# Ver logs
docker-compose logs -f app
```

## Desenvolvimento

### **Estrutura do Projeto**
```
nimble-gateway/
├── src/
│   ├── main/java/          # Código fonte (69 classes)
│   ├── main/resources/     # Configurações e migrações
│   ├── test/java/          # Testes (18 classes)
│   └── test/resources/     # Configurações de teste
├── target/                 # Build output
├── docker-compose.yml      # Docker Compose
├── Dockerfile             # Docker image
├── pom.xml                # Maven configuration
└── README.md              # Este arquivo
```

### **Padrões de Código**
- **Clean Code** - Código limpo e legível
- **SOLID** - Princípios de design orientado a objetos
- **DRY** - Don't Repeat Yourself
- **KISS** - Keep It Simple, Stupid
- **Domain-Driven Design** - Modelagem baseada no domínio

### **Ferramentas de Desenvolvimento**
- **Lombok** - Redução de boilerplate
- **MapStruct** - Mapeamento automático de DTOs
- **Bean Validation** - Validação declarativa
- **Spring Boot DevTools** - Desenvolvimento ágil

### **Convenções**
- **Nomenclatura**: camelCase para métodos, PascalCase para classes
- **Documentação**: JavaDoc para APIs públicas
- **Logs**: Estruturados com níveis apropriados
- **Exceções**: Hierarquia clara de exceções de domínio
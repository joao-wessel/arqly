# Arqly

Fundação SaaS para gerenciamento de projetos de arquitetura.

## Stack

- Backend: Java 21, Spring Boot 3, Maven, Spring Security, JWT, JPA/Hibernate, Flyway, PostgreSQL.
- Frontend: Angular 20 standalone, Tailwind CSS, Lucide Icons, ApexCharts e FullCalendar preparados.
- Infra: Docker Compose com `frontend`, `backend` e `postgres`.

## Acesso inicial

O backend cria um administrador da plataforma no bootstrap:

- E-mail: `admin@arqly.local`
- Senha: `Admin@12345`

Rotas principais:

- Admin: `http://localhost:4200/login/admin`
- Tenant: `http://localhost:4200/login`

## Executar

```bash
docker compose up -d --build
```

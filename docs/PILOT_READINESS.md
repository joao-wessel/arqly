# Readiness de Staging e Piloto

## Escopo e estado

Este documento é o roteiro operacional para a VPS real. Ele não substitui evidências: cada item deve receber data, responsável, URL/ID de execução e resultado antes da recomendação de piloto.

O ambiente local validou build, Flyway, persistência, backup e restore em containers temporários. Não há acesso neste workspace ao domínio, VPS, SMTP ou destino externo de backup; portanto, o status atual é **NO-GO para piloto com dados reais** até que os itens externos e a homologação autenticada sejam concluídos.

## Evidências locais concluídas

| Validação | Evidência | Resultado |
| --- | --- | --- |
| Configuração Docker de staging/HTTPS | `docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml config --quiet` | APROVADO |
| Backend Java 21 | `mvn test` em imagem `maven:3.9.9-eclipse-temurin-21` | 22 testes aprovados |
| Frontend | `npm test -- --watch=false` | 10 testes aprovados |
| Build do frontend | `npm run build -- --configuration production` | APROVADO |
| Flyway em banco vazio | stack temporária com migrations até `V31` | APROVADO |
| Health | `/actuator/health` na stack temporária | APROVADO |
| Proxy e SPA | raiz, rota SPA após refresh e API protegida sem token | APROVADO localmente |
| Persistência | recriação de containers temporários preservando PostgreSQL e uploads | APROVADO |
| Disaster recovery | backup de PostgreSQL e uploads, remoção dos volumes temporários e restore | APROVADO localmente |

Essas evidências não substituem a execução na VPS real. A composição local de desenvolvimento, quando usada com portas de conveniência, não é evidência de segurança de staging; somente `docker-compose.staging.yml` e seu override HTTPS devem ser usados no servidor.

## Ambiente da VPS

Preencher no staging real:

| Item | Evidência esperada | Status |
| --- | --- | --- |
| Domínio e DNS | `A/AAAA` resolve para a VPS | PENDENTE |
| Firewall | somente SSH, 80 e 443 expostos | PENDENTE |
| Serviços | `docker compose ... ps` saudável | PENDENTE |
| HTTPS | certificado válido, hostname correto e redirect HTTP->HTTPS | PENDENTE |
| Actuator | saúde acessível somente pela rede interna | PENDENTE |
| Volumes | PostgreSQL e uploads persistentes | VALIDADO LOCALMENTE |
| SMTP | teste de e-mail ou pendência explícita | PENDENTE |
| Backup externo | cópia fora da VPS confirmada | BLOQUEADOR |

## Subida e validação operacional

1. Criar `/opt/arqly/app/.env` a partir de `.env.example`, preencher valores reais e executar `chmod 600 .env`.
2. Executar `docker compose -f docker-compose.staging.yml build`.
3. Executar `docker compose -f docker-compose.staging.yml up -d`.
4. Confirmar PostgreSQL e backend saudáveis com `docker compose ... ps` e `docker compose ... logs backend`.
5. Confirmar `200` no frontend, refresh de uma rota SPA e `401/403` em uma API protegida sem token.
6. Com DNS propagado, emitir o certificado conforme o README e subir a composição HTTPS.
7. Confirmar `http://DOMINIO` redirecionando para HTTPS, certificado válido e `certbot renew --dry-run`.
8. Confirmar externamente que `5432` e `8080` não respondem e que somente SSH, 80 e 443 estão publicados.

## Smoke autenticado obrigatório

Usando apenas dados fictícios e o fluxo oficial da aplicação:

1. Habilitar temporariamente o bootstrap de plataforma ou utilizar um administrador já existente; desabilitá-lo após a criação inicial.
2. Criar `Tenant Staging` e um administrador pelo painel/API oficial de plataforma.
3. Fazer login, abrir uma rota autenticada, sair e confirmar que a sessão deixa de acessar a rota.
4. Fazer upload de arquivo pequeno, baixar, criar nova versão e vincular arquivos a projeto e etapa.
5. Testar arquivo com nome contendo tentativa de traversal, como `../../arquivo.txt`; a aplicação deve bloquear ou sanitizar o nome.
6. Testar arquivo maior em ambiente controlado e confirmar resposta compreensível quando exceder o limite.

## Homologação funcional

Executar no tenant fictício e registrar resultado de cada fluxo:

| Módulo | Cenário mínimo | Status |
| --- | --- | --- |
| Cliente | criar, editar, consultar, buscar PF/PJ | PENDENTE |
| Briefing | requisitos, preferências, restrições, proposta | PENDENTE |
| Proposta | serviços, pagamento, aceite, financeiro sem duplicidade | PENDENTE |
| Financeiro | R$ 12.000 em 3 parcelas, baixa parcial, total e estorno; conta a pagar | PENDENTE |
| Projeto | modelo, responsável, fases, etapas e progresso | PENDENTE |
| Workspace | status, responsável, checklist, comentário e Activity | PENDENTE |
| Arquivos | upload, download, nova versão, projeto e etapa | PENDENTE |
| Documentos | template, placeholder, geração e versão | PENDENTE |
| Diário | rascunho, visita, participantes, ocorrência, decisão, foto e publicação | PENDENTE |
| Aprovações | solicitar, aprovar, rejeitar e Activity | PENDENTE |
| Agenda | manual, derivado, filtro e origem | PENDENTE |
| Notificações | atribuição, menção, rejeição e ocorrência crítica | PENDENTE |
| Home | atenção, agenda, projetos, indicadores, financeiro e permissões | PENDENTE |
| Busca | entidades, acentuação, navegação e isolamento | PENDENTE |
| Portal | cliente, visibilidade, arquivos, documentos e aprovações | PENDENTE |

## Segurança e isolamento

Criar Tenant A e Tenant B, ambos fictícios. Autenticado como Tenant A, testar IDs conhecidos do Tenant B em cliente, briefing, proposta, projeto, etapa, arquivo, documento, diário, financeiro, Activity e Agenda. O resultado deve ser `403` ou `404`, nunca dados.

Também testar associação manipulada: criar projeto no Tenant A usando `clientId` do Tenant B, além das associações equivalentes em proposta, arquivo, financeiro e etapa. Criar usuário administrador e usuário comum; validar que Financeiro e ações administrativas não dependem apenas de botão escondido.

No Portal, testar token válido, expirado e revogado, além de tentativa de acessar projeto ou arquivo de outro cliente. Arquivos, documentos, Activities e Diário internos nunca podem aparecer ao cliente.

## Observabilidade e falhas

Durante os testes, conferir `X-Request-Id`, logs sem senha/JWT completo/secret e stack trace apenas no servidor. Simular backend indisponível, sessão expirada, falha de upload, `403` e `500`; o frontend não deve exibir detalhes técnicos.

Criar volume razoável de dados fictícios antes da verificação final, observando Home, Busca, Projetos, Activity Feed e Financeiro. Registrar endpoints lentos, N+1 evidente ou consultas repetidas como `HIGH` ou `MEDIUM`; corrigir somente problemas comprovados.

## Backup e recuperação no staging real

1. Definir `ARQLY_BACKUP_DIR=/opt/arqly/backups` e executar `./scripts/backup.sh`.
2. Conferir `./scripts/backup-status.sh`, manifest, checksums e permissões.
3. Configurar cron diário e executar `./scripts/cleanup-backups.sh --dry-run` antes da primeira limpeza automática.
4. Configurar cópia externa via `scripts/copy-backup.sh`; uma cópia fora da VPS é obrigatória antes de dados reais.
5. Restaurar ao menos um backup gerado no staging em um ambiente temporário paralelo ou janela planejada; validar banco, `storage_key` e download de arquivo.

## Resultado e critérios de decisão

### Blockers automáticos

- vazamento cross-tenant ou pelo Portal;
- bypass de autenticação;
- perda de banco ou uploads;
- restore inválido;
- migration destrutiva;
- inconsistência financeira;
- armazenamento não persistente;
- ausência de cópia de backup fora da VPS antes de dados reais.

### Resultado atual

**NO-GO PARA PILOTO.** A base técnica e o disaster recovery local foram validados, mas não há evidência de VPS real, DNS/HTTPS, firewall, SMTP, backup externo nem homologação autenticada ponta a ponta. Após preencher todos os itens acima sem blockers, este documento deve ser atualizado para `GO PARA PILOTO`.

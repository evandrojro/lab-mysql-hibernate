# Tx Lab – MySQL + Hibernate

Este projeto é um **laboratório de estudos** focado em entender, na prática, como funcionam **transações**, **propagation** e **locks** usando **Spring Boot, JPA/Hibernate e MySQL**.

A ideia aqui não é construir um sistema completo ou pronto para produção, mas sim um ambiente controlado para simular cenários reais e observar o comportamento do banco e do Hibernate em diferentes situações.

---

## 🎯 Objetivo

Explorar, com exemplos simples, situações que normalmente só ficam claras quando algo dá errado em produção, como:

- Commits e rollbacks
- Transações aninhadas
- Exceções dentro e fora de `@Transactional`
- Diferenças entre tipos de propagation
- Concorrência, bloqueios e deadlocks no MySQL

---

## 🧠 O que é explorado

- `@Transactional` e seus tipos de **propagation**
- Transações externas chamando transações internas
- Rollback automático vs. manual
- Efeito de exceções no fluxo transacional
- Locks pessimistas (`SELECT FOR UPDATE`)
- Concorrência real entre requisições
- Deadlocks causados por ordem diferente de locks

---

## 🧪 Como usar o projeto

Os endpoints expostos servem apenas como **gatilho** para executar cenários específicos de teste.

Cada endpoint representa um caso de estudo, por exemplo:
- uma transação segurando um lock por muito tempo
- outra transação bloqueando até o lock ser liberado
- dois fluxos concorrentes gerando deadlock

Nada aqui foi pensado para produção. O foco é **aprendizado e experimentação**.

---

## 🔗 Workspace público no Postman

As collections com os cenários de teste estão disponíveis neste workspace público do Postman:

👉 https://www.postman.com/brobet/workspace/lab-study/collection/45943117-c9755cdc-5890-4ff9-9733-f44356ade4d1

---

## ⚙️ Tecnologias

- Java 21  
- Spring Boot  
- Spring Data JPA / Hibernate  
- MySQL  
- Flyway  

---

## ⚠️ Observação final

Se algum comportamento parecer estranho ou “errado”, provavelmente foi feito assim de propósito 😄  
Este projeto existe justamente para testar limites e entender efeitos colaterais de transações e locks.

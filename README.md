# Tx Lab – MySQL + Hibernate

Este projeto é um **laboratório de estudos** focado em entender, na prática, como funcionam **transações**, **propagation** e **locks** usando **Spring Boot, Hibernate e MySQL**.

A ideia não é construir um sistema completo ou pronto para produção, mas sim um ambiente controlado para simular cenários reais e observar o comportamento do banco e do Hibernate em diferentes situações.

---

## 🎯 Objetivo

Usar código simples para explorar conceitos que normalmente só ficam claros quando algo dá errado em produção, como:

- Commits e rollbacks
- Transações aninhadas
- Exceções dentro e fora de `@Transactional`
- Diferenças entre tipos de propagation
- Concorrência e locks no MySQL

---

## 🧠 O que é explorado

- `@Transactional` e seus tipos de **propagation**
- Transações externas chamando transações internas
- Rollback automático vs. manual
- Efeito de exceções no fluxo transacional
- Locks de banco (`SELECT FOR UPDATE`, concorrência, deadlocks)
- Difer

# Raport - Projekt ORM

## Temat projektu

Implementacja systemu ORM (Object-Relational Mapping) w Kotlinie

## Status rozwoju

- Zaprojektowano i zaimplementowano podstawową strukturę systemu ORM
- System wspiera bazę danych SQLite
- Zaimplementowano wzorce projektowe: Builder, Command, Factory, Singleton

## Główne funkcjonalności

### 1. Mapowanie obiektowo-relacyjne

- Automatyczne mapowanie klas Kotlin na tabele w bazie danych
- Wsparcie dla podstawowych typów danych (INTEGER, VARCHAR, BOOLEAN, DECIMAL, TEXT)
- Elastyczna definicja kluczy głównych oraz ograniczeń

### 2. System zapytań

- API do budowania zapytań
- Bezpieczne parametryzowane zapytania
- Podstawowe operacje CRUD

### 3. Zarządzanie połączeniami

- Singleton pattern dla fabryki połączeń dla różnych providerów baz danych

## Ciekawe aspekty techniczne

- Wykorzystanie Kotlin reflection do automatycznego mapowania
- Implementacja własnego systemu budowania zapytań SQL
- Type-safe API dzięki typom generycznym

## Napotkane wyzwania

- Prawidłowa obsługa różnych typów danych SQL i róznic między danymi providerami baz danych
- Zapewnienie bezpieczeństwa przy wykonywaniu zapytań

## Plany rozwoju

- Wsparcie większej ilości typów SQL (np. Date)
- Dodanie obsługi pozostałych SQL Statement (Update, Delete itp.)
- Dodanie wsparcia dla relacji między tabelami (one-to-one, one-to-many)
- Rozszerzenie o wspracie PostgreSQL i MySQL
- Dokończenie modelu mapowania obiektów na rzędy

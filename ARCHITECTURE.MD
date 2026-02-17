# 🏛️ TSD Platform Architecture

This document outlines the High-Level Architecture of the TSD Platform, following **Hexagonal Architecture** (Ports & Adapters) principles.

## 🗺️ System Map

The system is designed as a **Unified Platform** where business features "plug in" to a robust workflow engine.

```mermaid
graph TD
    %% --- STYLING ---
    classDef foundation fill:#2c3e50,stroke:#fff,stroke-width:0px,color:#fff;
    classDef engine fill:#95a5a6,stroke:#7f8c8d,stroke-width:1px,color:#000;
    classDef core fill:#f1c40f,stroke:#f39c12,stroke-width:2px,color:#000;
    classDef feature fill:#3498db,stroke:#2980b9,stroke-width:2px,color:#fff;
    classDef adapter fill:#ecf0f1,stroke:#bdc3c7,stroke-width:2px,stroke-dasharray: 5 5,color:#000;

    %% ============================================================
    %% 1. THE BUSINESS FEATURES (The Roof)
    %% ============================================================
    subgraph Features [BUSINESS FEATURES (com.tsd.features)]
        direction LR
        %% These are your specific business rules
        F1(Audit)
        F2(Calculation)
        F3(Compliance)
        F4(Settlement)
        F5(CorporateEvent)
    end
    class Features feature
    class F1,F2,F3,F4,F5 feature

    %% ============================================================
    %% 2. THE CORE DOMAIN (The Pillars)
    %% ============================================================
    subgraph Core [CORE DOMAIN (com.tsd.core)]
        direction TB
        %% The Pure Logic that Features use
        C1(Identity Service)
        C2(Consensus Service)
        C3(Account Model)
    end
    class Core core
    class C1,C2,C3 core

    %% ============================================================
    %% 3. THE ENGINE (The Floor)
    %% ============================================================
    subgraph Platform [PLATFORM ENGINE (com.tsd.platform)]
        direction TB
        %% The generic machinery
        P1(EnterpriseWorkflowEngine)
        P2(ConfigMatrix)
        P3(SecurityConfig)
    end
    class Platform engine
    class P1,P2,P3 engine

    %% ============================================================
    %% 4. THE INFRASTRUCTURE (The Foundation)
    %% ============================================================
    subgraph Infra [INFRASTRUCTURE (com.tsd.adapter)]
        direction LR
        %% The dirty plumbing
        I1(Web Controllers)
        I2(SQL Repositories)
        I3(OneID Proxy)
    end
    class Infra foundation
    class I1,I2,I3 foundation

    %% ============================================================
    %% CONNECTIONS (Gravity)
    %% ============================================================
    
    %% Features depend on Core
    F1 --> C3
    F2 --> C3
    F3 --> C1
    F4 --> C3
    F5 --> C2

    %% Core depends on Platform interfaces (conceptually)
    Core -. runs on .- Platform

    %% Platform uses Infra to save state
    P1 --> I2
    C1 --> I3

    %% Infra drives the whole thing
    I1 --> P1
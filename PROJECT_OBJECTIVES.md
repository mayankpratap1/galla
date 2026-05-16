# EdgeLLM Pro - Enterprise Android LLM Client

## Project Overview

**Project Name:** EdgeLLM Pro
**Goal:** Production-ready Android app for on-device LLM inference with enterprise architecture.

---

## Architecture: Clean Architecture (MVVM + Use Cases)

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Screens   │  │  ViewModels │  │   UI Components     │ │
│  │  (Compose)  │  │  (Hilt DI)  │  │   (Reusable)        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Models    │  │  Use Cases  │  │ Repository Interfaces│ │
│  │ (Entities)  │  │  (Business) │  │   (Abstractions)     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  API Clients│  │ Repositories│  │   Data Sources      │ │
│  │ (HF, Cloud)│  │  (Impl)      │  │ (Local, Remote)     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       CORE LAYER                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Error     │  │   Network   │  │   DI (Hilt)         │ │
│  │  Handling   │  │   Client    │  │   Modules           │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) |
| Network | OkHttp + Retrofit |
| Database | Room |
| Storage | DataStore Preferences |
| Architecture | Clean Architecture + MVVM |
| Async | Kotlin Coroutines + Flow |

---

## Current State (v0.2 - Enterprise Refactor)

### ✅ Completed (Current Sprint)
- [x] Clean Architecture folder structure
- [x] Hilt dependency injection setup
- [x] Domain layer: Models, Repository interfaces, Use Cases
- [x] Data layer: API clients, Repository implementations
- [x] Core layer: Error handling (Failure/Resource pattern), Network client
- [x] PreferencesManager with DataStore
- [x] Updated build.gradle.kts with Hilt, Retrofit, Accompanist
- [x] GalleryViewModel with Hilt DI + Domain layer
- [x] New MainApp with Navigation + proper theming

### ❌ Remaining Work

---

## Phase 1: Engine Infrastructure

### 1.1 GGUF Engine
- [x] kotlinllamacpp dependency
- [x] GgufEngine implementation
- [ ] Model file picker with SAF support
- [ ] Memory management

### 1.2 LiteRT-LM Engine
- [x] Google LiteRT-LM SDK
- [x] LiteRtEngine implementation
- [ ] GPU/NPU acceleration toggle
- [ ] Model cache management

---

## Phase 2: Model Gallery (Hugging Face)

### 2.1 API Integration ✅
- [x] HuggingFaceApi with OkHttp
- [x] Search by query, library, task
- [x] Model details + files

### 2.2 Download Manager
- [x] DownloadRepository with progress
- [ ] Resume support (Range headers)
- [x] Storage validation before download
- [ ] Concurrent downloads queue

### 2.3 Model Manager
- [x] List installed models
- [x] Delete model
- [x] Storage display
- [ ] Model info screen

### 2.4 UI
- [x] Browse tab with filters
- [x] Search functionality
- [x] Installed models tab
- [ ] Model detail bottom sheet
- [ ] Download progress UI

---

## Phase 3: Cloud API Fallback

### 3.1 Cloud Repository
- [ ] CloudService interface
- [ ] API key encrypted storage

### 3.2 Providers
- [ ] OpenAI (GPT-4o, o1)
- [ ] Anthropic (Claude 3.5)
- [ ] Google Gemini 2.0
- [ ] xAI Grok

### 3.3 Multimodal
- [ ] Image upload (vision)
- [ ] Audio input (STT)

---

## Phase 4: Feature Completions

### 4.1 Chat
- [ ] Room DB persistence
- [ ] Export (Markdown/PDF)
- [ ] System prompts
- [ ] Temperature presets

### 4.2 Vision (Ask Image)
- [ ] Camera capture
- [ ] Gallery picker
- [ ] Image preprocessing
- [ ] History

### 4.3 Audio (Speech-to-Text)
- [ ] Recording
- [ ] Transcription
- [ ] Export

### 4.4 Benchmark
- [ ] MMLU-style tests
- [ ] Custom prompts
- [ ] Performance metrics

### 4.5 Prompt Lab
- [ ] Templates library
- [ ] Testing interface
- [ ] Export/Import

### 4.6 Agent Skills
- [ ] Skill marketplace
- [ ] URL installation

---

## Phase 5: UI/UX

### 5.1 Navigation
- [x] Bottom nav (Done)
- [ ] Drawer menu
- [ ] Model quick-switcher

### 5.2 Settings
- [x] Theme support
- [ ] API key management
- [ ] Network settings

### 5.3 Onboarding
- [ ] First-launch guide
- [ ] Device detection

---

## Phase 6: Performance & Polish

### 6.1 Performance
- [ ] Model preloading
- [ ] Memory optimization

### 6.2 Security
- [x] Encrypted storage (Done)
- [ ] Network config

### 6.3 Stability
- [x] Error handling (Done)
- [ ] Crash reporting

---

## Phase 7: Build & Release

### 7.1 Build
- [x] Gradle 8.11.1 (Done)
- [ ] Proguard/R8
- [ ] App signing

### 7.2 CI/CD
- [x] GitHub Actions (Done)

---

## File Structure (Clean Architecture)

```
app/src/main/java/com/edgellm/
├── core/
│   ├── di/           # Hilt modules
│   ├── error/       # Failure, Resource classes
│   ├── network/     # OkHttp client
│   └── util/        # Format utilities
├── data/
│   ├── local/       # Preferences, DAOs
│   ├── remote/     # API clients, DTOs
│   └── repository/ # Repository implementations
├── domain/
│   ├── model/       # Domain entities
│   ├── repository/ # Repository interfaces
│   └── usecase/     # Business logic use cases
├── features/
│   ├── gallery/     # Model gallery feature
│   ├── chat/       # Chat feature
│   ├── vision/     # Ask Image feature
│   ├── audio/      # Audio Scribe feature
│   ├── benchmark/  # Benchmark feature
│   ├── skills/     # Agent Skills feature
│   ├── promptlab/  # Prompt Lab feature
│   └── settings/   # Settings feature
└── presentation/
    ├── ui/
    │   ├── components/  # Reusable UI components
    │   ├── theme/      # Material 3 theme
    │   └── MainApp.kt  # Main navigation
    └── viewmodel/      # Shared ViewModels
```

---

## Progress

| Phase | Status | Progress |
|-------|--------|----------|
| Architecture | Done | 100% |
| Phase 1 | In Progress | 70% |
| Phase 2 | In Progress | 75% |
| Phase 3 | Not Started | 0% |
| Phase 4 | Not Started | 0% |
| Phase 5 | In Progress | 30% |
| Phase 6 | Not Started | 0% |
| Phase 7 | In Progress | 20% |

---

## For Next AI Session

### Immediately Ready
- All Hilt modules configured
- Domain layer complete with Use Cases
- Data layer with Repository implementations
- GalleryViewModel with DI

### Next Tasks
1. Fix build errors (if any from refactor)
2. Complete GalleryScreen with detail view
3. Add Cloud API fallback
4. Complete remaining features

### Key Files to Continue
- `core/di/AppModule.kt` - DI configuration
- `domain/usecase/` - Business logic
- `features/gallery/` - Model gallery feature
- `presentation/ui/` - UI layer

---

Last Updated: 2026-05-16
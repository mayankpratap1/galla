# EdgeLLM Pro - Project Objectives

## Project Overview

**Project Name:** EdgeLLM Pro
**Goal:** Build a production-ready Android app for on-device LLM inference with multiple engines and model repositories.

---

## Current State (v0.1)

### ✅ Completed
- Basic Android app structure with Compose UI
- InferenceEngine interface with GgufEngine and LiteRtEngine
- EngineFactory for engine selection
- Basic UI screens (Chat, AskImage, AudioScribe, Benchmark, AgentSkills, PromptLab, Settings)
- Ktor-based local API server
- Background service (EdgeLLMService)

### ❌ Missing / TODO

---

## Phase 1: Core Engine Infrastructure ✅ (In Progress)

### 1.1 GGUF Engine Integration
- [x] kotlinllamacpp dependency
- [x] GgufEngine implementation
- [ ] Model file picker with SAF support
- [ ] Memory management for large models

### 1.2 LiteRT-LM Engine Integration
- [x] Google LiteRT-LM SDK dependency
- [x] LiteRtEngine implementation
- [ ] Model file copy to internal storage
- [ ] GPU/NPU acceleration toggle

---

## Phase 2: Model Gallery (Hugging Face Integration) ✅ (In Progress)

### 2.1 Hugging Face API Client
- [x] HF Hub API integration (custom OkHttp client)
- [x] Model search by: name, task (text-generation), library (gguf, litertlm)
- [x] Model metadata fetching (size, downloads, likes, quantization)
- [x] Featured/Trending models endpoint
- [x] GGUF and LiteRT file detection

### 2.2 Download Manager
- [x] DownloadService with progress tracking
- [ ] Resume support for interrupted downloads
- [ ] Storage space validation before download
- [ ] Concurrent download queue
- [ ] File integrity verification (SHA256)

### 2.3 Model Manager
- [x] Installed models list with metadata
- [x] Delete models
- [x] Storage usage display
- [ ] Model info screen (version, size, quantizations)

### 2.4 Model Gallery UI
- [x] Browse models by category (Trending, GGUF, LiteRT, Chat, Vision)
- [x] Search with filters
- [ ] Model detail page with quantizations
- [x] Download button with progress
- [x] Installed models tab with delete option

---

## Phase 3: Cloud API Fallback (Reference: PrivateLM) 🔲

### 3.1 Cloud Service Abstraction
- [ ] Unified CloudService interface
- [ ] API key storage (encrypted)
- [ ] Provider selection UI

### 3.2 Supported Providers
- [ ] OpenAI (GPT-4, GPT-4o, GPT-4o-mini)
- [ ] Anthropic (Claude 3.5 Sonnet)
- [ ] Google Gemini
- [ ] xAI (Grok)

### 3.3 Multimodal Support
- [ ] Image upload for vision models
- [ ] Audio input support

---

## Phase 4: Feature Completions (Edge Gallery Features) 🔲

### 4.1 Chat Enhancements
- [ ] Chat history persistence (Room DB)
- [ ] Export chat as Markdown/PDF
- [ ] System prompt customization
- [ ] Temperature/top-p presets

### 4.2 Ask Image (Vision)
- [ ] Camera integration
- [ ] Gallery image picker
- [ ] Image preprocessing (resize, compress)
- [ ] History of analyzed images

### 4.3 Audio Scribe (Speech-to-Text)
- [ ] Audio recording
- [ ] Whisper integration or cloud STT
- [ ] Transcript export

### 4.4 Benchmark
- [ ] Standard benchmarks (MMLU-style questions)
- [ ] Custom prompt testing
- [ ] Performance metrics (tokens/sec, latency)
- [ ] Device tier detection

### 4.5 Prompt Lab
- [ ] Prompt templates library
- [ ] Prompt testing interface
- [ ] Export/import prompts
- [ ] Prompt optimization suggestions

### 4.6 Agent Skills
- [ ] Skill marketplace
- [ ] Skill installation from URL/markdown
- [ ] Skill execution in chat

---

## Phase 5: UI/UX Enhancements 🔲

### 5.1 Navigation
- [ ] Bottom navigation bar
- [ ] Drawer menu for settings
- [ ] Model quick-switcher

### 5.2 Settings
- [ ] Theme (light/dark/system)
- [ ] Default engine selection
- [ ] Memory management settings
- [ ] API key management
- [ ] Network settings (proxy, timeout)

### 5.3 Onboarding
- [ ] First-launch model download guide
- [ ] Device capability detection
- [ ] Recommended settings

---

## Phase 6: Performance & Polish 🔲

### 6.1 Performance
- [ ] Memory optimization
- [ ] Model preloading
- [ ] Caching (GPU kernels for LiteRT)

### 6.2 Security
- [ ] Encrypted storage for API keys
- [ ] Secure file handling
- [ ] Network security config

### 6.3 Stability
- [ ] Error handling & user feedback
- [ ] Crash reporting
- [ ] Graceful degradation

---

## Phase 7: Build & Distribution 🔲

### 7.1 Build Configuration
- [x] Gradle 8.11.1 (DONE)
- [ ] Build variants (debug/release)
- [ ] Proguard/R8 minification
- [ ] App signing

### 7.2 CI/CD
- [x] GitHub Actions workflow (android.yml)
- [ ] Build verification
- [ ] Test automation

---

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Framework | Android SDK 35 |
| Local Models | kotlinllamacpp (GGUF), LiteRT-LM SDK |
| HTTP Client | OkHttp, Ktor |
| Database | Room |
| DI | Manual (CompositionLocal) |
| Storage | DataStore (preferences), EncryptedSharedPreferences |

---

## Notes

- Model Gallery will use Hugging Face Hub API
- Cloud fallback follows PrivateLM architecture
- All features must work offline when models are downloaded
- Memory management critical for large language models

---

## Progress Tracker

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1 | In Progress | 80% |
| Phase 2 | In Progress | 70% |
| Phase 3 | Not Started | 0% |
| Phase 4 | Not Started | 0% |
| Phase 5 | Not Started | 0% |
| Phase 6 | Not Started | 0% |
| Phase 7 | In Progress | 10% |

---

## Files Created/Modified (for AI handoff)

### New Files
- `app/src/main/java/com/edgellm/huggingface/HuggingFaceModels.kt` - Data models for HF API
- `app/src/main/java/com/edgellm/huggingface/HuggingFaceApiClient.kt` - HF API HTTP client
- `app/src/main/java/com/edgellm/download/ModelDownloadService.kt` - Download manager with progress
- `app/src/main/java/com/edgellm/features/gallery/GalleryViewModel.kt` - Gallery state management
- `app/src/main/java/com/edgellm/features/gallery/GalleryScreen.kt` - Gallery UI (Browse + Installed)
- `PROJECT_OBJECTIVES.md` - This objectives file

### Modified Files
- `app/build.gradle.kts` - Added DataStore, Security-Crypto dependencies
- `app/src/main/java/com/edgellm/ui/MainNavigation.kt` - Added Gallery route

---

Last Updated: 2026-05-16
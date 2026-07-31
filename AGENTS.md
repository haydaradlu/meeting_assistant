# Meeting Assistant — Project Documentation

> **Tujuan**: File ini dibuat agar agent AI dapat memahami keseluruhan project tanpa perlu membaca ulang seluruh codebase.

## Ringkasan Project

**Meeting Assistant** adalah aplikasi skripsi untuk **perangkuman otomatis notulensi rapat** menggunakan algoritma **TextRank** yang dipadukan dengan model embedding **Multilingual-E5-Small**. Sistem terdiri dari 2 komponen utama:

1. **Backend** — Python FastAPI REST API (`E:\Tes Aplikasi\Aplikasi\backend`)
2. **Android** — Kotlin Android Native app (`E:\Tes Aplikasi\Aplikasi\MeetingAssistant`)

### Alur Utama Aplikasi

```
Audio Rapat 🎙️
    ↓ (upload dari Android)
OpenAI Whisper (Speech-to-Text)
    ↓ (transkripsi teks dengan tanda baca)
TextRank + E5-Small (Extractive Summarization)
    ↓ (ringkasan otomatis)
Hasil ditampilkan di Android App
    ↓ (validasi oleh Admin/Pemimpin Rapat)
Laporan dikirim oleh Notulis (jika status "valid")
```

---

## Arsitektur Sistem

```
┌─────────────────────────────────────────────────────────┐
│                    Android App (Kotlin)                  │
│  LoginActivity → DashboardActivity → Fragments          │
│  Retrofit + OkHttp → API calls to backend               │
│  BASE_URL: http://10.0.2.2:8000/api/ (emulator)        │
└───────────────────────┬─────────────────────────────────┘
                        │ REST API (JSON)
                        ▼
┌─────────────────────────────────────────────────────────┐
│                 Backend (Python FastAPI)                  │
│  Uvicorn → app.main:app                                  │
│  Port: 8000                                              │
│                                                          │
│  Services:                                               │
│    WhisperService → Speech-to-Text (model: small)        │
│    TextRankService → Extractive Summarization             │
│                                                          │
│  Database: PostgreSQL                                    │
│    URL: postgresql://postgres:postgres@localhost:5432/    │
│         meeting_assistant                                │
└─────────────────────────────────────────────────────────┘
```

---

## Backend — Detail Teknis

### Lokasi: `E:\Tes Aplikasi\Aplikasi\backend`

### Tech Stack
- **Framework**: FastAPI
- **Server**: Uvicorn (`uvicorn app.main:app --reload --host 0.0.0.0 --port 8000`)
- **Database**: PostgreSQL via SQLAlchemy ORM
- **Auth**: JWT (python-jose) + bcrypt
- **AI/ML**: OpenAI Whisper, SentenceTransformers (E5-Small), NetworkX, scikit-learn
- **Virtual Environment**: `E:\Tes Aplikasi\Aplikasi\backend\venv`

### Struktur Direktori

```
backend/
├── app/
│   ├── main.py                    # FastAPI app entry point, lifespan, CORS, routers
│   ├── config.py                  # Settings dari .env (DB URL, JWT, Whisper config)
│   ├── database.py                # SQLAlchemy engine, SessionLocal, Base, get_db
│   │
│   ├── models/                    # SQLAlchemy ORM models
│   │   ├── admin.py               # Admin(admin_id, username, password, name)
│   │   ├── pemimpin_rapat.py      # PemimpinRapat(pr_id, username, password, name)
│   │   ├── notulis.py             # Notulis(notulis_id, username, password, name)
│   │   ├── rekaman_rapat.py       # RekamanRapat(rec_id, notulis_id, file_audio, tanggal, nama_rekaman, created_by)
│   │   ├── hasil_transkripsi.py   # HasilTranskripsi(hasil_id, rec_id, pr_id, notulis_id, hasil_transkripsi, hasil_ringkasan, summary_percentage, status_validasi, tanggal)
│   │   └── laporan.py             # LaporanHasilTranskripsi(laporan_id, hasil_id, pr_id, admin_id, file_laporan, tanggal_kirim)
│   │
│   ├── schemas/                   # Pydantic schemas (request/response models)
│   │   ├── auth.py                # LoginRequest, LoginResponse, TokenData
│   │   ├── admin.py               # AdminCreate, AdminUpdate, AdminResponse
│   │   ├── pemimpin_rapat.py      # PRCreate, PRUpdate, PRResponse
│   │   ├── notulis.py             # NotulisCreate, NotulisUpdate, NotulisResponse
│   │   ├── rekaman_rapat.py       # RekamanResponse
│   │   ├── hasil_transkripsi.py   # HasilCreate, HasilUpdate, HasilResponse, ValidateRequest
│   │   └── laporan.py             # LaporanCreate, LaporanResponse
│   │
│   ├── routers/                   # FastAPI routers (REST endpoints)
│   │   ├── auth.py                # POST /api/auth/login
│   │   ├── admin.py               # CRUD /api/admin
│   │   ├── pemimpin_rapat.py      # CRUD /api/pemimpin-rapat
│   │   ├── notulis.py             # CRUD /api/notulis
│   │   ├── rekaman_rapat.py       # CRUD /api/rekaman + background AI pipeline
│   │   ├── hasil_transkripsi.py   # CRUD /api/hasil + transcribe + summarize + validate
│   │   └── laporan.py             # CRUD /api/laporan + download
│   │
│   ├── services/                  # AI/ML services
│   │   ├── whisper_service.py     # WhisperService — STT dengan initial_prompt untuk tanda baca
│   │   └── textrank_service.py    # TextRankService — Extractive summarization
│   │
│   └── utils/
│       └── auth.py                # hash_password, verify_password, create_access_token, get_current_user, require_role
│
├── uploads/                       # File uploads
│   ├── audio/                     # Audio files (.wav, .mp3, .m4a, etc.)
│   └── laporan/                   # Generated report files (.txt)
│
├── database/                      # SQL scripts
│   ├── schema.sql                 # DDL: tables, enums, triggers (update_updated_at)
│   ├── seed.sql                   # Default user accounts (admin, pemimpin, notulis)
│   ├── fix_passwords.sql          # Reset passwords with bcrypt hashes
│   └── add_created_by.sql         # Migration: add created_by to rekaman_rapat
│
├── requirements.txt               # Python dependencies
└── venv/                          # Virtual environment
```

### API Endpoints

| Prefix | Deskripsi | Auth |
|--------|-----------|------|
| `POST /api/auth/login` | Login, cek admin → pemimpin_rapat → notulis secara berurutan | No |
| `/api/admin` | CRUD admin accounts | Yes |
| `/api/pemimpin-rapat` | CRUD pemimpin rapat accounts | Yes |
| `/api/notulis` | CRUD notulis accounts | Yes |
| `/api/rekaman` | Upload audio + auto-create HasilTranskripsi + trigger background AI pipeline | Yes |
| `/api/hasil` | CRUD hasil transkripsi, manual transcribe/summarize, validate status | Yes |
| `/api/laporan` | CRUD laporan, download file | Yes |

### AI Pipeline (Background Task)

Saat rekaman baru di-upload via `POST /api/rekaman`:

```python
# Lokasi: rekaman_rapat.py → process_audio_in_background()
1. Whisper STT → transcript = whisper_service.transcribe(audio_path)
2. TextRank    → summary = textrank_service.summarize(transcript, percentage)
3. Save to DB  → hasil.hasil_transkripsi = transcript
                  hasil.hasil_ringkasan = summary
```

### TextRank Algorithm (Sesuai Perhitungan Manual Skripsi Bab IV)

Lokasi: `app/services/textrank_service.py`

```
1. Sentence segmentation   → NLTK sent_tokenize()
2. Preprocessing            → case folding → remove special chars → normalize space
3. Prefix attachment        → "query: " + cleaned_sentence
4. Embedding                → intfloat/multilingual-e5-small (384 dimensi)
5. Similarity matrix        → cosine_similarity, diagonal = 0
6. TextRank/PageRank        → nx.pagerank(graph, alpha=0.85)
7. Select top N%            → round(total * percentage / 100) kalimat
8. Output                   → kalimat asli (bukan preprocessed) dalam urutan kronologis
```

**PENTING**: Whisper menggunakan `initial_prompt` dengan tanda baca agar output mengandung punctuation. Tanpa ini, seluruh teks dianggap 1 kalimat dan tidak bisa diringkas.

### Konfigurasi (config.py / .env)

| Setting | Default | Keterangan |
|---------|---------|------------|
| `DATABASE_URL` | `postgresql://postgres:admin1212@localhost:5432/meeting_assistant` | PostgreSQL connection (password di .env: `admin1212`) |
| `JWT_SECRET_KEY` | `your-super-secret-key-change-in-production` | JWT signing key |
| `JWT_ALGORITHM` | `HS256` | JWT algorithm |
| `JWT_EXPIRATION_MINUTES` | `1440` (24 jam) | Token expiry |
| `WHISPER_MODEL` | `small` | Whisper model size |
| `WHISPER_LANGUAGE` | `id` | Indonesian |
| `SUMMARY_PERCENTAGE` | `30` | Default compression rate |
| `UPLOAD_DIR` | `uploads` | Upload directory |

### Library Paths (External/Cached)

| Library | Path |
|---------|------|
| Whisper models | `E:\Untuk Library\whisper` |
| SentenceTransformers | `E:\Untuk Library\sentence_transformers` |
| HuggingFace cache | `E:\Untuk Library\huggingface` |

---

## Android App — Detail Teknis

### Lokasi: `E:\Tes Aplikasi\Aplikasi\MeetingAssistant`
### Package: `com.nexsoft.meetingassistant`

### Tech Stack
- **Language**: Kotlin
- **Min SDK**: 24 | **Target SDK**: 34
- **View Binding**: Enabled
- **HTTP Client**: Retrofit 2 + OkHttp + Gson
- **UI**: Material Design, DrawerLayout, Fragments

### Struktur Direktori

```
app/src/main/java/com/nexsoft/meetingassistant/
├── utils/
│   ├── Constants.kt          # BASE_URL, SharedPrefs keys, role constants
│   └── SessionManager.kt     # Auth session management via SharedPreferences
│
├── api/
│   ├── ApiClient.kt          # Retrofit singleton, auth interceptor, 60s timeout
│   └── ApiService.kt         # Retrofit interface — semua endpoint definitions
│
├── models/                   # Data classes (Gson serialization)
│   ├── Admin.kt              # adminId, username, password, name
│   ├── PemimpinRapat.kt      # prId, username, password, name
│   ├── Notulis.kt            # notulisId, username, password, name
│   ├── RekamanRapat.kt       # recId, notulisId, notulisName, fileAudio, tanggal, namaRekaman
│   ├── HasilTranskripsi.kt   # hasilId, recId, hasilTranskripsi, hasilRingkasan, statusValidasi, notulisName, ...
│   ├── Laporan.kt            # laporanId, hasilId, fileLaporan, tanggalKirim, ...
│   ├── LoginRequest.kt       # username, password
│   ├── LoginResponse.kt      # accessToken, tokenType, role, userId, name
│   └── ApiResponse.kt        # Generic wrapper ApiResponse<T>
│
├── adapters/                 # RecyclerView adapters
│   ├── AdminAdapter.kt
│   ├── PemimpinRapatAdapter.kt
│   ├── NotulisAdapter.kt
│   ├── RekamanAdapter.kt     # Hides delete button for role notulis
│   ├── HasilListAdapter.kt
│   └── LaporanAdapter.kt
│
└── ui/                       # Activities & Fragments
    ├── LoginActivity.kt      # Login screen (LAUNCHER), session check, password toggle
    ├── DashboardActivity.kt  # Main activity, DrawerLayout, role-based nav menus, fragment host
    ├── AdminFragment.kt      # CRUD admin with search, add/edit dialog, self-delete protection
    ├── PemimpinRapatFragment.kt  # CRUD pemimpin rapat
    ├── NotulisFragment.kt    # CRUD notulis
    ├── RekamanRapatFragment.kt   # Upload audio, pick file, date picker
    ├── HasilTranskripsiListFragment.kt  # List hasil + search + status filter
    ├── HasilTranskripsiDetailFragment.kt # Detail view, validate, kirim laporan (only if valid)
    └── LaporanFragment.kt    # List laporan, download, delete
```

### Role-Based Access Control (3 Roles)

| Role | Menu yang Tersedia | Aksi Khusus |
|------|-------------------|-------------|
| **admin** | Admin, Pemimpin Rapat, Notulis, Rekaman Rapat, Hasil Transkripsi, Laporan | Manage semua users, validate status, hapus data |
| **pemimpin_rapat** | Rekaman Rapat, Hasil Transkripsi, Laporan | Validate status, hapus data |
| **notulis** | Rekaman Rapat, Hasil Transkripsi, Laporan | Upload rekaman, kirim laporan (hanya jika status = "valid") |

### Navigation Flow

```
LoginActivity
    ↓ (authenticated)
DashboardActivity
    ├── DrawerLayout (role-based menu)
    │   ├── nav_menu_admin.xml        → admin role
    │   ├── nav_menu_pemimpin_rapat.xml → pemimpin_rapat role
    │   └── nav_menu_notulis.xml      → notulis role
    │
    └── fragmentContainer
        ├── AdminFragment
        ├── PemimpinRapatFragment
        ├── NotulisFragment
        ├── RekamanRapatFragment
        ├── HasilTranskripsiListFragment
        │       ↓ (view detail)
        │   HasilTranskripsiDetailFragment
        └── LaporanFragment
```

### Key UI Behaviors

1. **Sidebar (DrawerLayout)** — Saat dibuka, halaman utama menjadi overlay gelap
2. **Tombol Kirim Laporan** — Disabled (alpha 0.5) jika status bukan "valid"
3. **Notulis field** — Menampilkan `notulisName` dari backend (fallback ke `created_by` dari rekaman)
4. **Hasil Transkripsi** — Menampilkan ringkasan (TextRank). Fallback ke transkripsi mentah jika ringkasan belum siap
5. **Self-delete protection** — User tidak bisa menghapus akun sendiri

---

## Database Schema (PostgreSQL)

```
┌──────────────┐     ┌────────────────────────┐     ┌───────────────────────────┐
│    admin     │     │    pemimpin_rapat       │     │         notulis           │
├──────────────┤     ├────────────────────────┤     ├───────────────────────────┤
│ admin_id PK  │     │ pr_id PK               │     │ notulis_id PK             │
│ username UK  │     │ username UK             │     │ username UK               │
│ password     │     │ password                │     │ password                  │
│ name         │     │ name                    │     │ name                      │
│ created_at   │     │ created_at              │     │ created_at                │
│ updated_at   │     │ updated_at              │     │ updated_at                │
└──────────────┘     └────────────────────────┘     └──────────┬────────────────┘
                                                               │
                                                    ┌──────────▼────────────────┐
                                                    │      rekaman_rapat        │
                                                    ├───────────────────────────┤
                                                    │ rec_id PK                 │
                                                    │ notulis_id FK → notulis   │
                                                    │ file_audio                │
                                                    │ tanggal                   │
                                                    │ nama_rekaman              │
                                                    │ created_by                │
                                                    │ created_at                │
                                                    │ updated_at                │
                                                    └──────────┬────────────────┘
                                                               │
                                                    ┌──────────▼────────────────┐
                                                    │   hasil_transkripsi       │
                                                    ├───────────────────────────┤
                                                    │ hasil_id PK               │
                                                    │ rec_id FK → rekaman_rapat │
                                                    │ pr_id FK → pemimpin_rapat │
                                                    │ notulis_id FK → notulis   │
                                                    │ hasil_transkripsi TEXT     │
                                                    │ hasil_ringkasan TEXT      │
                                                    │ summary_percentage        │
                                                    │ tanggal                   │
                                                    │ status_validasi ENUM      │
                                                    │   (pending/valid/tidak)   │
                                                    │ created_at                │
                                                    │ updated_at                │
                                                    └──────────┬────────────────┘
                                                               │
                                                    ┌──────────▼────────────────┐
                                                    │ laporan_hasil_transkripsi │
                                                    ├───────────────────────────┤
                                                    │ laporan_id PK             │
                                                    │ hasil_id FK → hasil       │
                                                    │ pr_id FK → pemimpin_rapat │
                                                    │ admin_id FK → admin       │
                                                    │ file_laporan              │
                                                    │ tanggal_kirim             │
                                                    │ created_at                │
                                                    │ updated_at                │
                                                    └───────────────────────────┘
```

### StatusValidasi Enum
- `pending` — default saat rekaman baru di-upload
- `valid` — disetujui oleh admin/pemimpin rapat
- `tidak_valid` — ditolak

---

## Cara Menjalankan

### 1. Menggunakan Docker Compose (Rekomendasi & Deployment VPS)

Persyaratan: [Docker Desktop](https://www.docker.com/) (Lokal) atau Docker Engine + Docker Compose Plugin (VPS Linux).

```bash
# Jalankan dari direktori utama project (E:\Tes Aplikasi\Aplikasi)
docker compose up -d --build
```

**Penjelasan Container & Network**:
- `db` (PostgreSQL 15): Berjalan pada port `5432`. Otomatis menginisialisasi skema tabel (`schema.sql`) dan akun awal (`seed.sql`).
- `backend` (FastAPI + AI Services): Berjalan pada port `8000`. Terhubung langsung ke kontainer `db` via internal Docker network (`meeting_net`).
- File rekaman audio & laporan disimpan persisten di `./backend/uploads/`.
- Cache model AI (Whisper & E5-Small) disimpan persisten pada volume `model_cache` sehingga tidak perlu diunduh ulang setiap container direstart.

```bash
# Cek status container
docker compose ps

# Cek log aplikasi backend real-time
docker compose logs -f backend

# Menghentikan container
docker compose down
```

### 2. Menggunakan Environment Lokal (Manual)

```bash
cd E:\Tes Aplikasi\Aplikasi\backend
.\venv\Scripts\activate
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

**Prasyarat**: PostgreSQL berjalan di `localhost:5432` dengan database `meeting_assistant`.

### 3. Android App

1. Buka `E:\Tes Aplikasi\Aplikasi\MeetingAssistant` di Android Studio
2. Sync Gradle
3. Run pada emulator (pastikan backend sudah berjalan)
4. Emulator mengakses backend via `http://10.0.2.2:8000/api/` (atau IP Domain/VPS jika di-deploy ke server).

---

## Referensi Perhitungan Manual

Lokasi: `E:\Tes Aplikasi\Perhitungan Manual\Bab 5 19 juli malam4-179-191.pdf`

PDF ini berisi perhitungan manual TextRank dari Bab IV skripsi dengan contoh 6 kalimat (S1-S6, di mana S6 adalah noise tentang cuaca). Implementasi kode harus sesuai dengan langkah-langkah di dokumen ini:

1. Sentence segmentation menggunakan NLTK
2. Preprocessing: case folding → remove special characters → normalize space
3. Prefix: `"query: "` (bukan `"passage: "`)
4. Embedding: multilingual-e5-small (384 dimensi)
5. Cosine similarity matrix, diagonal = 0
6. PageRank/TextRank dengan damping factor d = 0.85
7. Seleksi top-N% kalimat menggunakan `round()` (bukan `int()`)
8. Output dalam urutan kronologis (urutan kemunculan asli)

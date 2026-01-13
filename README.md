# MachinaEar

<div align="center">

**Intelligent Predictive Maintenance for Small-Scale Industrial Machinery**

[![Next.js](https://img.shields.io/badge/Next.js-15.0-black?logo=next.js)](https://nextjs.org/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta_EE-10.0-orange?logo=eclipse)](https://jakarta.ee/)
[![WildFly](https://img.shields.io/badge/WildFly-Server-red)](https://www.wildfly.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Database-green?logo=mongodb)](https://www.mongodb.com/)
[![PyTorch](https://img.shields.io/badge/PyTorch-Lightning-orange?logo=pytorch)](https://pytorch.org/)

</div>

---

## 🎯 Overview

MachinaEar is an enterprise-grade predictive maintenance system designed specifically for small-scale industrial environments. By leveraging AI-powered anomaly detection on vibration and acoustic data, MachinaEar enables proactive maintenance strategies that minimize downtime and reduce repair costs.

The platform combines edge computing (Raspberry Pi 4), cloud-based machine learning, and a modern Progressive Web App to deliver real-time machine health insights directly to users' devices.

### Key Features

- 🔊 **Real-time Anomaly Detection** - Continuous 24/7 monitoring using AI models
- 📱 **Progressive Web App** - Cross-platform mobile & desktop support (iOS, Android, Windows, macOS, Linux)
- 🔐 **OAuth 2.1 Security** - Enterprise-grade authentication with PKCE & refresh token rotation
- 🔒 **Two-Factor Authentication (2FA)** - TOTP-based security with recovery codes
- 📊 **Edge Computing** - On-device ML inference on Raspberry Pi 4 using TensorFlow Lite
- 🔄 **Automated Model Retraining** - CI/CD pipeline with MLflow for continuous improvement
- 💬 **MQTT Integration** - Real-time device communication and data streaming

---

## 📋 Table of Contents

- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Authentication & Security](#-authentication--security)
- [Machine Learning](#-machine-learning)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Deployment](#-deployment)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🏗 Architecture

MachinaEar follows a modern three-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT LAYER (PWA)                      │
│  Next.js + TypeScript + TailwindCSS + Service Workers      │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  APPLICATION LAYER (API)                    │
│    Jakarta EE 10 + WildFly + JAX-RS + WebSocket           │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    DATA LAYER                               │
│         MongoDB (Users, Devices, Tokens)                    │
└─────────────────────────────────────────────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    EDGE DEVICES (IoT)                       │
│  Raspberry Pi 4 + TensorFlow Lite + MQTT + Sensors         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠 Technology Stack

### Frontend (Client)

- **Framework:** Next.js 15.0 with React 19
- **Language:** TypeScript 5
- **Styling:** TailwindCSS 3.4
- **PWA:** @ducanh2912/next-pwa with service workers
- **State Management:** React Hooks + Context API
- **Icons:** Heroicons & Lucide React
- **Authentication:** JWT with OAuth 2.1 + PKCE

### Backend (Server)

- **Framework:** Jakarta EE 10 (JAX-RS, CDI, EJB)
- **Application Server:** WildFly
- **Language:** Java 21
- **Build Tool:** Maven 3.x
- **Database:** MongoDB 4.11+
- **Authentication:** OAuth 2.1, JWT (Nimbus JOSE+JWT), Argon2 password hashing
- **2FA:** Google Authenticator (TOTP), QR Code generation (ZXing)
- **Messaging:** Eclipse Paho MQTT Client
- **API Documentation:** MicroProfile OpenAPI (Swagger)

### Machine Learning (Edge & Cloud)

- **Framework:** PyTorch Lightning
- **Model Architecture:** 3-layer CNN Autoencoder
- **Feature Extraction:** Mel Spectrograms (librosa)
- **Edge Deployment:** TensorFlow Lite on Raspberry Pi 4
- **MLOps:** MLflow for experiment tracking
- **CI/CD:** GitHub Actions

### DevOps & Infrastructure

- **Version Control:** Git + GitHub
- **CI/CD:** GitHub Actions
- **Deployment:** Docker-ready, WildFly containers
- **Monitoring:** Application logs + MLflow tracking

---

## 📁 Project Structure

```
MachinaEar/
├── client/                          # Next.js Progressive Web App
│   ├── app/                         # App Router pages
│   │   ├── auth/                    # Authentication pages
│   │   ├── devices/                 # Device management
│   │   ├── home/                    # Dashboard
│   │   ├── settings/                # User settings & 2FA
│   │   └── offline/                 # Offline fallback
│   ├── components/                  # Reusable React components
│   │   ├── devices/                 # Device UI components
│   │   ├── 2fa/                     # Two-factor authentication
│   │   └── ui/                      # Base UI components
│   ├── hooks/                       # Custom React hooks
│   ├── lib/                         # Utilities & API client
│   │   ├── api-client.ts            # HTTP client
│   │   ├── oauth.ts                 # OAuth 2.1 flow
│   │   ├── pkce.ts                  # PKCE implementation
│   │   └── validation.ts            # Form validation
│   ├── public/                      # Static assets & PWA files
│   │   ├── manifest.json            # PWA manifest
│   │   └── sw.js                    # Service worker
│   └── types/                       # TypeScript type definitions
│
├── MachinaEar/                      # Jakarta EE Backend
│   ├── src/main/java/MachinaEar/
│   │   ├── iam/                     # Identity & Access Management
│   │   │   ├── boundaries/          # REST API endpoints
│   │   │   ├── controllers/         # Business logic
│   │   │   │   ├── managers/        # Service managers
│   │   │   │   └── repositories/    # Data access layer
│   │   │   ├── entities/            # MongoDB entities
│   │   │   └── security/            # Security utilities
│   │   ├── devices/                 # IoT device management

│   │   └── mqtt/                    # MQTT client service
│   ├── src/main/webapp/
│   │   ├── WEB-INF/                 # Web app configuration
│   │   └── login.html               # Legacy login page
│   └── pom.xml                      # Maven dependencies
│
├── ml/                              # Machine Learning Module
│   ├── models/                      # Model architectures
│   │   └── cnn_autoencoder.py       # 3-layer CNN Autoencoder
│   ├── training/                    # Training scripts
│   │   └── train.py                 # Model training
│   ├── testing/                     # Evaluation scripts
│   │   └── test.py                  # Model testing
│   ├── utils/                       # ML utilities
│   │   ├── preprocessing.py         # Audio preprocessing
│   │   ├── pytorch_utils.py         # PyTorch helpers
│   │   └── common.py                # Common utilities
│   ├── notebooks/                   # Jupyter notebooks
│   │   └── data_eda.ipynb           # Exploratory analysis
│   ├── rpi/                         # Raspberry Pi deployment
│   │   ├── rpi_agent.py             # Main agent script
│   │   ├── rpi_agent_gpio.py        # GPIO sensor interface
│   │   └── requirements-rpi.txt     # RPi dependencies
│   ├── config.yaml                  # ML configuration
│   └── requirements.txt             # Python dependencies
│
├── docs/                            # Documentation
│   └── ScopeStatement.md            # Project scope & business model
│
├── scripts/                         # Deployment & utility scripts
│   ├── deploy-backend-code.js       # Backend deployment
│   ├── package-app.js               # Application packaging
│   └── server.js                    # Development server
│
├── certs/                           # SSL/TLS certificates
│   └── openssl.cnf                  # OpenSSL configuration
│
└── README.md                        # This file
```

---

## 🔐 Authentication & Security

MachinaEar implements **OAuth 2.1** as the primary authentication protocol with enterprise-grade security features.

### OAuth 2.1 Compliance

- ✅ **Authorization Code Flow with PKCE (S256)** - Required for all clients
- ✅ **Refresh Token Rotation** - Automatic token rotation with revocation
- ✅ **State Parameter** - CSRF protection on authorization requests
- ✅ **Single-Use Authorization Codes** - Codes expire after 10 minutes
- ✅ **Secure Token Storage** - Refresh tokens hashed with SHA-256

### Two-Factor Authentication (2FA)

- 🔐 **TOTP-based** - Time-based One-Time Password (RFC 6238)
- 📱 **Google Authenticator Compatible** - Works with any TOTP app
- 🔑 **Recovery Codes** - 10 single-use backup codes
- 📊 **QR Code Setup** - Easy enrollment via QR scanning

### Authentication Flow

```
1. User → Login Page (credentials)
2. Frontend → PKCE generation (code_verifier, code_challenge)
3. Frontend → POST /auth/oauth/login (credentials + PKCE)
4. Backend → Validates credentials (Argon2 hash verification)
5. Backend → Creates session & redirects to /auth/authorize
6. Backend → Generates authorization code (10-min expiry)
7. Frontend → Callback receives code
8. Frontend → POST /auth/token (code + code_verifier)
9. Backend → Validates PKCE & issues tokens
10. Frontend → Stores tokens (localStorage or httpOnly cookies)
11. Backend → Saves hashed refresh token in MongoDB
```

### Key Security Endpoints

| Endpoint             | Method | Description                              |
| -------------------- | ------ | ---------------------------------------- |
| `/auth/oauth/login`  | POST   | OAuth login with credentials             |
| `/auth/authorize`    | GET    | Authorization endpoint (code generation) |
| `/auth/token`        | POST   | Token endpoint (code exchange & refresh) |
| `/auth/register`     | POST   | User registration                        |
| `/auth/logout`       | POST   | Logout & token revocation                |
| `/auth/2fa/setup`    | POST   | Generate 2FA QR code                     |
| `/auth/2fa/verify`   | POST   | Verify TOTP code                         |
| `/auth/2fa/recovery` | POST   | Use recovery code                        |

### Security Implementation Files

#### Frontend

- [pkce.ts](client/lib/pkce.ts) - PKCE code generation
- [oauth.ts](client/lib/oauth.ts) - OAuth flow orchestration
- [auth/callback/page.tsx](client/app/auth/callback/page.tsx) - Callback handler

#### Backend

- `PhoenixIAMManager.java` - Token rotation & validation
- `RefreshTokenRepository.java` - Secure token storage
- `OAuth*Boundary.java` - REST endpoints
- `TOTPManager.java` - 2FA TOTP logic
- `RecoveryCodeRepository.java` - Recovery codes management

---

## 🤖 Machine Learning

MachinaEar uses a **3-layer CNN Autoencoder** for unsupervised anomaly detection in audio and vibration data.

### Model Architecture

```
Input: Mel Spectrogram (128 mel bands × 5 frames)
    ↓
Encoder:
  Conv2D(32) → BatchNorm → ReLU → MaxPool
  Conv2D(64) → BatchNorm → ReLU → MaxPool
  Conv2D(128) → BatchNorm → ReLU → MaxPool
    ↓
Latent Space (compressed representation)
    ↓
Decoder:
  ConvTranspose2D(128) → BatchNorm → ReLU → Upsample
  ConvTranspose2D(64) → BatchNorm → ReLU → Upsample
  ConvTranspose2D(32) → BatchNorm → ReLU → Upsample
  Conv2D(1) → Sigmoid
    ↓
Output: Reconstructed Spectrogram
```

### Feature Extraction

- **Input:** Raw audio WAV files from sensors
- **Preprocessing:**
  - FFT with window size 1024, hop length 512
  - 128 mel frequency bands
  - Power normalization (power=2.0)
- **Output:** Mel spectrogram (128×5 frames)

### Training Configuration

```yaml
feature:
  n_mels: 128
  frames: 5
  n_fft: 1024
  hop_length: 512
  power: 2.0

fit:
  lr: 0.001
  epochs: 100
  batch_size: 256
  validation_split: 0.1
```

### Anomaly Detection

- **Metric:** Mean Squared Error (MSE) between input and reconstruction
- **Threshold:** Determined from validation set (99th percentile)
- **Decision:** If MSE > threshold → Anomaly detected

### Edge Deployment

The trained PyTorch model is converted to **TensorFlow Lite** for efficient inference on Raspberry Pi 4:

```bash
# Model conversion
python convert_to_tflite.py --model checkpoints/best.ckpt --output model.tflite

# Deploy to Raspberry Pi
scp model.tflite pi@raspberrypi:/home/pi/machinaear/models/
```

### MLOps Pipeline

1. **Data Collection:** Sensors → MQTT → Cloud storage
2. **Model Training:** GitHub Actions triggers training on new data
3. **Experiment Tracking:** MLflow logs metrics, parameters, artifacts
4. **Model Versioning:** Best models tagged and deployed
5. **Edge Update:** New models pushed to Raspberry Pi devices via MQTT

---

## 🚀 Getting Started

### Prerequisites

#### Backend Requirements

- **Java Development Kit (JDK):** 21 or higher
- **Apache Maven:** 3.8+
- **WildFly Application Server:** 27+ (Jakarta EE 10 compatible)
- **MongoDB:** 4.11+ (local or cloud instance)

#### Frontend Requirements

- **Node.js:** 16.x or higher
- **npm:** 8.x or higher

#### Machine Learning Requirements (Optional)

- **Python:** 3.8 - 3.11
- **pip:** Latest version
- **CUDA:** 11.x+ (for GPU training, optional)

### Installation

#### 1️⃣ Clone the Repository

```bash
git clone https://github.com/yourusername/MachinaEar.git
cd MachinaEar
```

#### 2️⃣ Setup Backend (Jakarta EE)

```bash
# Navigate to backend directory
cd MachinaEar

# Configure MongoDB connection
# Edit src/main/resources/application.properties (if exists) or configure in WildFly

# Clean and build the project
mvn clean package

# Deploy to WildFly
mvn wildfly:run

# Alternative: Deploy WAR file manually
# Copy target/iam-0.1.0.war to WildFly deployments directory
```

The backend will be available at: **http://localhost:8080**

#### 3️⃣ Setup Frontend (Next.js PWA)

```bash
# Navigate to client directory
cd ../client

# Install dependencies
npm install

# Configure environment variables
cp .env.example .env.local
# Edit .env.local with your backend URL and other settings

# Development mode
npm run dev

# Production build
npm run build
npm run start
```

The frontend will be available at: **http://localhost:3000**

#### 4️⃣ Setup Machine Learning (Optional)

```bash
# Navigate to ML directory
cd ../ml

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure training
# Edit config.yaml with your data paths

# Run training
python training/train.py

# Run testing
python testing/test.py
```

### Configuration

#### Backend Configuration

Create `standalone.xml` datasource in WildFly:

```xml
<datasource jndi-name="java:jboss/datasources/MachinaEarDS" pool-name="MachinaEarDS">
    <connection-url>mongodb://localhost:27017/machinaear</connection-url>
    <driver>mongodb</driver>
</datasource>
```

#### Frontend Configuration

Create `.env.local` in `client/` directory:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_OAUTH_CLIENT_ID=machinaear-client
NEXT_PUBLIC_OAUTH_REDIRECT_URI=http://localhost:3000/auth/callback
NEXT_PUBLIC_ENABLE_PWA=true
```

#### MongoDB Collections

MachinaEar uses the following MongoDB collections:

- `users` - User accounts & authentication
- `refresh_tokens` - OAuth refresh tokens
- `devices` - IoT device registry
- `recovery_codes` - 2FA recovery codes
- `totp_secrets` - 2FA TOTP secrets

---

## 📖 API Documentation

MachinaEar exposes a RESTful API documented with **OpenAPI (Swagger)**.

### Swagger UI

Once the backend is running, access interactive API documentation at:

```
http://localhost:8080/openapi
```

### Core API Endpoints

#### Authentication

```http
POST   /api/auth/register           # Register new user
POST   /api/auth/oauth/login        # OAuth login with credentials
GET    /api/auth/authorize          # OAuth authorization endpoint
POST   /api/auth/token              # Token exchange & refresh
POST   /api/auth/logout             # Logout & revoke tokens
GET    /api/auth/me                 # Get current user info
```

#### Two-Factor Authentication

```http
POST   /api/auth/2fa/setup          # Generate 2FA QR code
POST   /api/auth/2fa/verify         # Verify TOTP code
POST   /api/auth/2fa/disable        # Disable 2FA
GET    /api/auth/2fa/recovery       # Get recovery codes
POST   /api/auth/2fa/recovery       # Use recovery code
```

#### Devices

```http
GET    /api/devices                 # List all devices
GET    /api/devices/{id}            # Get device details
POST   /api/devices                 # Register new device
PUT    /api/devices/{id}            # Update device
DELETE /api/devices/{id}            # Remove device
GET    /api/devices/{id}/status     # Get device status
GET    /api/devices/{id}/data       # Get sensor data
```

### Authentication

All API requests (except `/auth/register` and `/auth/oauth/login`) require a valid **JWT access token**:

```http
Authorization: Bearer <access_token>
```

---

## 🌐 Deployment

### Docker Deployment (Recommended)

```bash
# Build Docker images
docker-compose build

# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f
```

### Manual Deployment

#### Backend Deployment

```bash
# Build WAR file
cd MachinaEar
mvn clean package

# Copy to WildFly
cp target/iam-0.1.0.war $WILDFLY_HOME/standalone/deployments/

# Start WildFly
$WILDFLY_HOME/bin/standalone.sh
```

#### Frontend Deployment

```bash
# Build production bundle
cd client
npm run build

# Start production server
npm run start

# Or deploy to Vercel/Netlify
vercel deploy --prod
```

### Raspberry Pi Edge Deployment

```bash
# SSH to Raspberry Pi
ssh pi@raspberrypi

# Install dependencies
sudo apt-get update
sudo apt-get install python3-pip python3-venv

# Clone repository
git clone https://github.com/yourusername/MachinaEar.git
cd MachinaEar/ml

# Install RPi-specific dependencies
pip3 install -r requirements-rpi.txt

# Run agent
python3 rpi/rpi_agent.py --config config.yaml
```

### Environment Variables

#### Production Frontend

```env
NEXT_PUBLIC_API_URL=https://api.machinaear.com
NEXT_PUBLIC_OAUTH_CLIENT_ID=machinaear-production
NEXT_PUBLIC_OAUTH_REDIRECT_URI=https://app.machinaear.com/auth/callback
```

#### Production Backend

Configure in WildFly `standalone.xml` or environment:

```xml
<system-properties>
    <property name="mongodb.uri" value="mongodb+srv://user:pass@cluster.mongodb.net/machinaear"/>
    <property name="jwt.secret" value="your-secure-secret-key"/>
    <property name="gemini.api.key" value="your-gemini-api-key"/>
</system-properties>
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork the repository**
2. **Create a feature branch:** `git checkout -b feature/amazing-feature`
3. **Commit your changes:** `git commit -m 'Add amazing feature'`
4. **Push to the branch:** `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Code Style

- **Java:** Follow Jakarta EE best practices, use descriptive names
- **TypeScript:** ESLint + Prettier configuration provided
- **Python:** PEP 8 style guide, type hints encouraged

### Testing

```bash
# Backend tests
cd MachinaEar
mvn test

# Frontend tests
cd client
npm test

# ML tests
cd ml
pytest testing/
```

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 👥 Team

- **Project Lead:** [Your Name]
- **Backend Development:** [Team Member]
- **Frontend Development:** [Team Member]
- **ML Engineering:** [Team Member]

---

## 📞 Contact & Support

- **Email:** support@machinaear.com
- **Documentation:** https://docs.machinaear.com
- **Issues:** https://github.com/yourusername/MachinaEar/issues
- **Discussions:** https://github.com/yourusername/MachinaEar/discussions

---

## 🙏 Acknowledgments

- DCASE 2020 Challenge for ML architecture inspiration
- Jakarta EE community for enterprise patterns
- Next.js team for PWA capabilities
- MongoDB for flexible data modeling
- Eclipse Paho for MQTT implementation

---

<div align="center">

**MachinaEar** - Democratizing Predictive Maintenance

Made with ❤️ by the MachinaEar Team

</div>

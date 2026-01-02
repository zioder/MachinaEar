# machinaear
MachinaEar is an intelligent predictive maintenance system that transforms low-cost vibration and sound sensors into powerful diagnostic tools for small-scale industrial machinery. By “listening” to motors, bearings, and moving parts, MachinaEar can detect early signs of failure before breakdowns occur.
## Authentication & Security

MachinaEar implements **OAuth 2.1** as the primary authentication protocol with the following security features:

### OAuth 2.1 Compliance
- ✅ **Authorization Code Flow with PKCE** (S256) - Required for all clients
- ✅ **Refresh Token Rotation** - Tokens are rotated on each refresh and old tokens are revoked
- ✅ **State Parameter** - CSRF protection on all authorization requests
- ✅ **Single-Use Authorization Codes** - Codes expire after 10 minutes and can only be used once
- ✅ **Secure Token Storage** - Refresh tokens are hashed (SHA-256) before storage

### Authentication Flow
1. User enters credentials on login page
2. Frontend initiates OAuth flow with PKCE parameters
3. Backend validates credentials via `/auth/oauth/login` endpoint
4. Backend creates authenticated session and redirects to `/auth/authorize`
5. Authorization endpoint generates authorization code
6. Frontend callback receives code and exchanges it for tokens via `/auth/token`
7. Access and refresh tokens are returned (and optionally stored in httpOnly cookies)
8. Refresh token is stored in database for rotation tracking

### Key Endpoints
- `POST /auth/oauth/login` - Unified OAuth login with credentials
- `GET /auth/authorize` - OAuth authorization endpoint
- `POST /auth/token` - Token endpoint (authorization_code & refresh_token grants)
- `POST /auth/register` - User registration
- `POST /auth/logout` - Logout and token revocation

### Technical Implementation
- **Frontend**: Next.js with TypeScript
  - PKCE implementation in [pkce.ts](client/lib/pkce.ts)
  - OAuth flow in [oauth.ts](client/lib/oauth.ts)
  - Login page uses `loginWithOAuth()` function
  - Callback handler at [auth/callback/page.tsx](client/app/auth/callback/page.tsx)
  
- **Backend**: Jakarta EE with WildFly
  - Token rotation in [PhoenixIAMManager.java](MachinaEar/src/main/java/MachinaEar/iam/controllers/managers/PhoenixIAMManager.java)
  - Refresh token storage in [RefreshTokenRepository.java](MachinaEar/src/main/java/MachinaEar/iam/controllers/repositories/RefreshTokenRepository.java)
  - OAuth endpoints in [boundaries/](MachinaEar/src/main/java/MachinaEar/iam/boundaries/) package
## Getting Started

### Prerequisites
- Node.js (v16 or higher)
- Maven
- WildFly application server

### Launch Instructions

#### Client Application
1. Navigate to the client directory:
   ```bash
   cd client
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Build and run the development server:
   ```bash
   npm run build
   npm run dev
   ```

The client will be available at `http://localhost:3000`

#### Backend Server
1. Navigate to the MachinaEar directory:
   ```bash
   cd MachinaEar
   ```

2. Clean, build, and deploy to WildFly:
   ```bash
   mvn clean package
   mvn wildfly:run
   ```

The server will be available at `http://localhost:8080`
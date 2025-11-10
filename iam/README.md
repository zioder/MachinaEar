# MachinaEar IAM Service

## Overview

The **MachinaEar Identity and Access Management (IAM)** service provides comprehensive authentication, authorization, and device management capabilities for the MachinaEar predictive maintenance platform. This service is built using **Jakarta EE** and runs on **WildFly** application server with **MongoDB** as the persistence layer.

## Features

### Authentication & Authorization
- ✅ User registration with email verification
- ✅ Secure password hashing using Argon2id
- ✅ OAuth 2.0 authorization code flow with PKCE
- ✅ JWT-based token authentication (Ed25519 signatures)
- ✅ Role-based access control (RBAC)
- ✅ Refresh token support

### User Management
- ✅ User registration and activation
- ✅ Profile management (view, update, delete)
- ✅ Password requirements enforcement
- ✅ Email-based account activation

### Device Management
- ✅ Register IoT monitoring devices (Raspberry Pi units)
- ✅ View all devices owned by a user
- ✅ Update device information (name, location, status)
- ✅ Remove devices
- ✅ Device heartbeat tracking (last seen timestamp)
- ✅ Device status management (ACTIVE, INACTIVE, MAINTENANCE)
- ✅ Support for multiple machine types (3D Printers, Servers, Routers, etc.)

## Architecture

### Technology Stack

- **Backend Framework**: Jakarta EE 10
- **Application Server**: WildFly 34
- **Database**: MongoDB
- **Security**: Argon2id password hashing, Ed25519 JWT signing
- **Build Tool**: Maven 3.x
- **Java Version**: 21 (or 11+)

### Package Structure

```
tn.machinaear.iam/
├── boundaries/         # REST API endpoints
│   ├── DeviceManagementEndpoint.java
│   ├── IdentityManagementEndpoint.java
│   ├── IdentityRegistrationEndpoint.java
│   ├── OAuthAuthorizationEndpoint.java
│   └── OAuthTokenEndpoint.java
├── entities/          # Domain entities
│   ├── Device.java
│   ├── Identity.java
│   └── Tenant.java
├── repositories/      # Data access layer
│   ├── DeviceRepository.java
│   ├── IdentityRepository.java
│   ├── TenantRepository.java
│   └── IamRepository.java
├── services/          # Business logic
│   ├── DeviceService.java
│   ├── IdentityServices.java
│   └── EmailService.java
├── security/          # Security utilities
│   ├── Argon2Utils.java
│   ├── AuthorizationCode.java
│   └── JwtManager.java
├── enums/            # Enumerations
│   └── Role.java
└── filters/          # HTTP filters
    └── CORSFilter.java
```

## Getting Started

### Prerequisites

- Java 21 (or Java 11+)
- Maven 3.6+
- MongoDB 4.4+ (running on localhost:27017 or configure in properties)
- WildFly 34 (automatically provisioned by Maven)

### Configuration

Edit `src/main/resources/META-INF/microprofile-config.properties`:

```properties
# MongoDB Configuration
jnosql.document.database=MachinaEar_IAM
jnosql.mongodb.host=localhost:27017

# Argon2 Password Hashing
argon2.saltLength=32
argon2.hashLength=128
argon2.iterations=23
argon2.memory=97579
argon2.threadNumber=2

# JWT Configuration
key.pair.lifetime.duration=10800
key.pair.cache.size=3
jwt.lifetime.duration=10800
jwt.issuer=urn:machinaear.iam
jwt.claim.roles=groups
jwt.realm=urn:machinaear:iam

# Email Service (SMTP)
smtp.host=smtp.gmail.com
smtp.port=587
smtp.username=your-email@gmail.com
smtp.password=your-app-password
smtp.starttls.enable=true

# Custom Roles
roles=Client,Admin
```

### Build and Run

#### 1. Build the application

```bash
cd iam
mvn clean package
```

This will:
- Compile the application
- Run tests
- Create a WAR file
- Provision a WildFly server with the application deployed in `target/server`

#### 2. Start the server

```bash
cd target/server
# On Windows:
bin\standalone.bat

# On Linux/Mac:
./bin/standalone.sh
```

#### 3. Access the application

The IAM service will be available at:
```
http://localhost:8080/iam-1.0/api/iam/
```

## API Documentation

### Base URL
```
http://localhost:8080/iam-1.0/api/iam
```

### Authentication Endpoints

#### 1. Register a New User

**POST** `/register`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=johndoe" \
  -d "email=john@example.com" \
  -d "password=SecurePass123!"
```

**Response**: HTML page with activation code input

#### 2. Activate Account

**POST** `/register/activate`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/register/activate \
  -d "code=123456"
```

#### 3. Login (OAuth Authorization)

**GET** `/authorize?client_id={client}&redirect_uri={uri}&response_type=code&scope={scope}&code_challenge={challenge}&code_challenge_method=S256`

**POST** `/login/authorization`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/login/authorization \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=johndoe" \
  -d "password=SecurePass123!"
```

#### 4. Get Access Token

**POST** `/oauth/token`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code={authorization_code}" \
  -d "code_verifier={verifier}"
```

**Response**:
```json
{
  "token_type": "Bearer",
  "access_token": "eyJhbGc...",
  "expires_in": 10800,
  "scope": "resource:read resource:write",
  "refresh_token": "eyJhbGc..."
}
```

### Identity Management Endpoints

#### 1. Get User Profile

**GET** `/identities/profile`

```bash
curl -X GET http://localhost:8080/iam-1.0/api/iam/identities/profile \
  -H "Authorization: Bearer {access_token}"
```

#### 2. Update Identity

**PUT** `/identities/{id}`

```bash
curl -X PUT http://localhost:8080/iam-1.0/api/iam/identities/{id} \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newusername",
    "email": "newemail@example.com"
  }' \
  -G --data-urlencode "currentPassword=OldPass123!" \
  --data-urlencode "newPassword=NewPass123!"
```

#### 3. Delete Identity

**DELETE** `/identities/{id}`

```bash
curl -X DELETE http://localhost:8080/iam-1.0/api/iam/identities/{id} \
  -H "Authorization: Bearer {access_token}"
```

### Device Management Endpoints

#### 1. Register a Device

**POST** `/devices`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/devices \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceName": "3D Printer Monitor",
    "machineType": "3D Printer",
    "location": "Workshop Floor 2",
    "hardwareSerial": "RPI-12345-ABCDE",
    "firmwareVersion": "1.0.5",
    "description": "Monitoring Prusa i3 MK3S+"
  }'
```

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "3D Printer Monitor",
  "machineType": "3D Printer",
  "location": "Workshop Floor 2",
  "status": "ACTIVE",
  "registrationDate": "2025-11-10T10:30:00",
  "message": "Device registered successfully"
}
```

#### 2. Get My Devices

**GET** `/devices/my`

```bash
curl -X GET http://localhost:8080/iam-1.0/api/iam/devices/my \
  -H "Authorization: Bearer {access_token}"
```

**Response**:
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "deviceName": "3D Printer Monitor",
    "machineType": "3D Printer",
    "location": "Workshop Floor 2",
    "status": "ACTIVE",
    "ownerId": "user123",
    "registrationDate": "2025-11-10T10:30:00",
    "lastSeen": "2025-11-10T12:45:00",
    "hardwareSerial": "RPI-12345-ABCDE",
    "firmwareVersion": "1.0.5",
    "description": "Monitoring Prusa i3 MK3S+"
  }
]
```

#### 3. Get Device by ID

**GET** `/devices/{id}`

```bash
curl -X GET http://localhost:8080/iam-1.0/api/iam/devices/{device_id} \
  -H "Authorization: Bearer {access_token}"
```

#### 4. Update Device

**PUT** `/devices/{id}`

```bash
curl -X PUT http://localhost:8080/iam-1.0/api/iam/devices/{device_id} \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceName": "Updated Device Name",
    "location": "New Location",
    "status": "MAINTENANCE",
    "description": "Updated description"
  }'
```

#### 5. Delete Device

**DELETE** `/devices/{id}`

```bash
curl -X DELETE http://localhost:8080/iam-1.0/api/iam/devices/{device_id} \
  -H "Authorization: Bearer {access_token}"
```

**Response**: `204 No Content`

#### 6. Update Device Heartbeat

**POST** `/devices/{id}/heartbeat`

```bash
curl -X POST http://localhost:8080/iam-1.0/api/iam/devices/{device_id}/heartbeat \
  -H "Authorization: Bearer {access_token}"
```

**Response**:
```json
{
  "message": "Device heartbeat updated successfully"
}
```

#### 7. Get Devices by Status

**GET** `/devices/status/{status}`

```bash
curl -X GET http://localhost:8080/iam-1.0/api/iam/devices/status/ACTIVE \
  -H "Authorization: Bearer {access_token}"
```

**Valid statuses**: `ACTIVE`, `INACTIVE`, `MAINTENANCE`

## Device Management Use Cases

### Typical Device Lifecycle

1. **User Registration** → User creates account and activates via email
2. **User Login** → User authenticates and receives JWT token
3. **Device Registration** → User registers Raspberry Pi device with machine details
4. **Device Monitoring** → Device periodically sends heartbeat (last seen update)
5. **Device Updates** → User updates device location, status, or configuration
6. **Device Removal** → User removes device when no longer needed

### Machine Types

The system supports various machine types:
- **3D Printers** (Prusa, Creality, etc.)
- **Servers** (File servers, compute nodes)
- **Routers** (Network equipment)
- **Pumps** (Industrial pumps)
- **Motors** (Electric motors, conveyors)
- **CNC Machines** (Mills, lathes)
- **Custom** (Any other machinery)

## Testing

### Run Unit Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Test Coverage

The project includes:
- ✅ Unit tests for services (`DeviceService`, `IdentityServices`)
- ✅ Unit tests for entities (`Device`, `Identity`)
- ✅ Integration tests for REST endpoints
- ✅ Mock-based testing using Mockito

## Security Features

### Password Requirements
- Minimum 8 characters
- At least one digit
- At least one special character
- Argon2id hashing with high security parameters

### JWT Tokens
- Ed25519 elliptic curve signatures
- 3-hour expiration (configurable)
- Refresh token support
- Multiple audience support

### OAuth 2.0 PKCE
- Authorization code flow
- PKCE (Proof Key for Code Exchange)
- SHA-256 code challenge

### Authorization
- Bearer token authentication
- User ownership verification for devices
- Role-based access control

## Database Schema

### Collections

#### `identities`
```json
{
  "_id": "uuid",
  "username": "string",
  "email": "string",
  "password": "string (hashed)",
  "creationDate": "timestamp",
  "role": "long (bitmask)",
  "scopes": "string",
  "isAccountActivated": "boolean"
}
```

#### `devices`
```json
{
  "_id": "uuid",
  "device_name": "string",
  "machine_type": "string",
  "location": "string",
  "status": "string (ACTIVE|INACTIVE|MAINTENANCE)",
  "owner_id": "string (ref: identities._id)",
  "registration_date": "timestamp",
  "last_seen": "timestamp",
  "firmware_version": "string",
  "hardware_serial": "string",
  "description": "string"
}
```

#### `tenants`
```json
{
  "_id": "string",
  "tenant_name": "string",
  "tenant_secret": "string",
  "redirect_uri": "string",
  "allowed_roles": "long (bitmask)",
  "required_scopes": "string",
  "supported_grant_types": "string"
}
```

## Troubleshooting

### Common Issues

**1. MongoDB Connection Error**
```
Solution: Ensure MongoDB is running on localhost:27017
$ mongod --dbpath /path/to/data
```

**2. Email Sending Fails**
```
Solution: Configure SMTP credentials in microprofile-config.properties
Use Gmail App Password for Gmail SMTP
```

**3. JWT Token Invalid**
```
Solution: Check token expiration and ensure clock sync
Tokens expire after 3 hours by default
```

**4. Device Registration Fails**
```
Solution: Ensure user is authenticated with valid Bearer token
Verify all required fields are provided
```

**5. Port 8080 Already in Use**
```
Solution: Change WildFly port or stop conflicting service
Edit standalone.xml to change HTTP port
```

## Development

### Project Structure
```
iam/
├── pom.xml                    # Maven configuration
├── README.md                  # This file
├── src/
│   ├── main/
│   │   ├── java/             # Application source code
│   │   ├── resources/        # Configuration files
│   │   │   ├── META-INF/
│   │   │   │   └── microprofile-config.properties
│   │   │   ├── Activate.html
│   │   │   ├── Login.html
│   │   │   └── Register.html
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           ├── beans.xml
│   │           └── jboss-web.xml
│   └── test/
│       └── java/             # Test source code
└── target/                   # Build output
    ├── iam-1.0.war          # Deployable WAR file
    └── server/              # Provisioned WildFly server
```

### Adding Custom Roles

Edit `microprofile-config.properties`:
```properties
roles=Client,Admin,Technician,Manager
```

Roles are stored as bitmasks, supporting up to 62 custom roles plus GUEST and ROOT.

## Contributing

When contributing to the IAM service:

1. Follow Jakarta EE best practices
2. Write comprehensive unit and integration tests
3. Update API documentation for new endpoints
4. Ensure backward compatibility
5. Use semantic versioning

## License

This project is part of the MachinaEar platform.

## Support

For issues and questions:
- Check the troubleshooting section
- Review API documentation
- Contact the MachinaEar development team

---

**MachinaEar** - Intelligent Predictive Maintenance for Small-Scale Industrial Machinery



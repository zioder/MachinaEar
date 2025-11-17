# How to Run MachinaEar IAM Application

## Prerequisites

1. **Java 21** (or Java 11+)
2. **Maven 3.6+**
3. **MongoDB 4.4+** (must be running)

## Step-by-Step Instructions

### 1. Start MongoDB

**On Windows:**
```powershell
# If MongoDB is installed as a service, it may already be running
# Check if it's running:
Get-Service MongoDB

# If not running, start it:
net start MongoDB

# Or if installed manually, run:
mongod --dbpath "C:\path\to\your\data\db"
```

**On Linux/Mac:**
```bash
# Start MongoDB service
sudo systemctl start mongod
# OR
sudo service mongodb start

# Or run directly:
mongod --dbpath /path/to/your/data/db
```

**Verify MongoDB is running:**
```bash
# Should connect successfully
mongo
# OR (for newer versions)
mongosh
```

### 2. Build the Application

```bash
cd iam
mvn clean package
```

This will:
- Compile the application
- Run tests
- Create a WAR file (`iam-1.0.war`)
- Provision a WildFly server in `target/server`

### 3. Start WildFly Server

**On Windows:**
```powershell
cd target\server
.\bin\standalone.bat
```

**On Linux/Mac:**
```bash
cd target/server
./bin/standalone.sh
```

Wait for the server to start. You should see:
```
WFLYSRV0025: WildFly 34.0.0.Final ... started in XXXXms
WFLYUT0021: Registered web context: '/' for server 'default-server'
```

### 4. Access the API


```
http://localhost:8080/api/iam
```

**Test the API is running:**

**Linux/Mac (Bash):**
```bash
# Simple health check (if available)
curl http://localhost:8080/api/iam/

# Or open in browser:
# http://localhost:8080/api/iam/
```

**Windows (PowerShell):**
```powershell
# Simple health check (if available)
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/"

# Or using curl.exe:
curl.exe http://localhost:8080/api/iam/

# Or open in browser:
# http://localhost:8080/api/iam/
```

## Quick API Test Examples

> **Note for PowerShell users:** For best results, use the single-line commands. When copying multi-line commands, make sure not to include any `>` prompt characters. If you encounter errors, try the single-line version instead.

### 1. Register a User

**Linux/Mac (Bash):**
```bash
curl -X POST http://localhost:8080/api/iam/register \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "email=test@example.com" \
  -d "password=SecurePass123!"
```

**Windows (PowerShell) - Single Line (Recommended):**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/register" -Method POST -ContentType "application/x-www-form-urlencoded" -Body "username=testuser&email=test@example.com&password=SecurePass123!"
```

**Windows (PowerShell) - Multi-line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/register" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=testuser&email=test@example.com&password=SecurePass123!"
```

**Or using curl.exe (if available) - Single Line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/register -H "Content-Type: application/x-www-form-urlencoded" -d "username=testuser" -d "email=test@example.com" -d "password=SecurePass123!"
```

**Or using curl.exe - Multi-line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/register `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "username=testuser" `
    -d "email=test@example.com" `
    -d "password=SecurePass123!"
```

> **Note**: If you get an error like "An identity with username 'X' already exists", see the [User Already Exists Error](#user-already-exists-error) section in Troubleshooting for solutions.

### 2. Activate Account

**Linux/Mac (Bash):**
```bash
# Use the activation code sent to email (or check logs)
curl -X POST http://localhost:8080/api/iam/register/activate \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=123456"
```

**Windows (PowerShell) - Single Line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/register/activate" -Method POST -ContentType "application/x-www-form-urlencoded" -Body "code=123456"
```

**Windows (PowerShell) - Multi-line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/register/activate" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "code=123456"
```

**Or using curl.exe - Single Line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/register/activate -H "Content-Type: application/x-www-form-urlencoded" -d "code=123456"
```

**Or using curl.exe - Multi-line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/register/activate `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "code=123456"
```

### 3. Login (Get Authorization Code)

**Linux/Mac (Bash):**
```bash
curl -X POST http://localhost:8080/api/iam/login/authorization \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=testuser" \
  -d "password=SecurePass123!"
```

**Windows (PowerShell) - Single Line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/login/authorization" -Method POST -ContentType "application/x-www-form-urlencoded" -Body "username=testuser&password=SecurePass123!"
```

**Windows (PowerShell) - Multi-line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/login/authorization" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=testuser&password=SecurePass123!"
```

**Or using curl.exe - Single Line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/login/authorization -H "Content-Type: application/x-www-form-urlencoded" -d "username=testuser" -d "password=SecurePass123!"
```

**Or using curl.exe - Multi-line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/login/authorization `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "username=testuser" `
    -d "password=SecurePass123!"
```

### 4. Get Access Token

**Linux/Mac (Bash):**
```bash
curl -X POST http://localhost:8080/api/iam/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code={authorization_code_from_step_3}" \
  -d "code_verifier={code_verifier}"
```

**Windows (PowerShell) - Single Line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/oauth/token" -Method POST -ContentType "application/x-www-form-urlencoded" -Body "grant_type=authorization_code&code={authorization_code_from_step_3}&code_verifier={code_verifier}"
```

**Windows (PowerShell) - Multi-line:**
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/oauth/token" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "grant_type=authorization_code&code={authorization_code_from_step_3}&code_verifier={code_verifier}"
```

**Or using curl.exe - Single Line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/oauth/token -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=authorization_code" -d "code={authorization_code_from_step_3}" -d "code_verifier={code_verifier}"
```

**Or using curl.exe - Multi-line:**
```powershell
curl.exe -X POST http://localhost:8080/api/iam/oauth/token `
    -H "Content-Type: application/x-www-form-urlencoded" `
    -d "grant_type=authorization_code" `
    -d "code={authorization_code_from_step_3}" `
    -d "code_verifier={code_verifier}"
```

### 5. Access Protected Endpoints

**Linux/Mac (Bash):**
```bash
# Get user profile
curl -X GET http://localhost:8080/api/iam/identities/profile \
  -H "Authorization: Bearer {access_token}"

# Register a device
curl -X POST http://localhost:8080/api/iam/devices \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceName": "Test Device",
    "machineType": "3D Printer",
    "location": "Workshop",
    "hardwareSerial": "TEST-001"
  }'
```

**Windows (PowerShell) - Single Line:**
```powershell
# Get user profile
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/identities/profile" -Method GET -Headers @{ "Authorization" = "Bearer {access_token}" }

# Register a device (two commands)
$deviceBody = @{ deviceName = "Test Device"; machineType = "3D Printer"; location = "Workshop"; hardwareSerial = "TEST-001" } | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/devices" -Method POST -Headers @{ "Authorization" = "Bearer {access_token}"; "Content-Type" = "application/json" } -Body $deviceBody
```

**Windows (PowerShell) - Multi-line:**
```powershell
# Get user profile
Invoke-WebRequest -Uri "http://localhost:8080/api/iam/identities/profile" `
    -Method GET `
    -Headers @{ "Authorization" = "Bearer {access_token}" }

# Register a device
$deviceBody = @{
    deviceName = "Test Device"
    machineType = "3D Printer"
    location = "Workshop"
    hardwareSerial = "TEST-001"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/iam/devices" `
    -Method POST `
    -Headers @{ 
        "Authorization" = "Bearer {access_token}"
        "Content-Type" = "application/json"
    } `
    -Body $deviceBody
```

**Or using curl.exe - Single Line:**
```powershell
# Get user profile
curl.exe -X GET http://localhost:8080/api/iam/identities/profile -H "Authorization: Bearer {access_token}"

# Register a device
curl.exe -X POST http://localhost:8080/api/iam/devices -H "Authorization: Bearer {access_token}" -H "Content-Type: application/json" -d "{\"deviceName\":\"Test Device\",\"machineType\":\"3D Printer\",\"location\":\"Workshop\",\"hardwareSerial\":\"TEST-001\"}"
```

**Or using curl.exe - Multi-line:**
```powershell
# Get user profile
curl.exe -X GET http://localhost:8080/api/iam/identities/profile `
    -H "Authorization: Bearer {access_token}"

# Register a device
curl.exe -X POST http://localhost:8080/api/iam/devices `
    -H "Authorization: Bearer {access_token}" `
    -H "Content-Type: application/json" `
    -d "{\"deviceName\":\"Test Device\",\"machineType\":\"3D Printer\",\"location\":\"Workshop\",\"hardwareSerial\":\"TEST-001\"}"
```

## Troubleshooting

### MongoDB Connection Issues
- **Error**: Cannot connect to MongoDB
- **Solution**: 
  - Verify MongoDB is running: `mongo` or `mongosh`
  - Check MongoDB host/port in `src/main/resources/META-INF/microprofile-config.properties`
  - Default: `localhost:27017`

### WildFly Port Already in Use
- **Error**: Address already in use on port 8080
- **Solution**:
  - Change port in WildFly configuration, OR
  - Stop other services using port 8080

### Application Not Deploying
- **Error**: Deployment failed
- **Solution**: 
  - Check server logs in `target/server/standalone/log/server.log`
  - Verify all dependencies are resolved: `mvn clean install`
  - Check MongoDB connection

### User Already Exists Error

**Error**: `An identity with username 'zied' already exists.` (or similar username/email)

This error occurs when trying to register a user with a username or email that is already in the database.

**Solutions**:

1. **Use a Different Username/Email (Recommended for Testing)**:
   - Simply use a different username and/or email when registering
   - Example: `username=zied2&email=zied2.kallel@supcom.tn`

2. **Delete User from MongoDB (Easiest for Development)**:
   
   Connect to MongoDB and delete the existing user:
   
   **Using MongoDB Shell (mongosh or mongo)**:
   ```javascript
   // Connect to MongoDB
   use MachinaEar_IAM
   
   // Find the user by username
   db.identities.find({username: "zied"})
   
   // Delete the user by username
   db.identities.deleteOne({username: "zied"})
   
   // Or delete by email
   db.identities.deleteOne({email: "zied.kallel@supcom.tn"})
   
   // Verify deletion
   db.identities.find({username: "zied"})
   ```
   
   **Using PowerShell with MongoDB Compass or MongoDB CLI**:
   - Open MongoDB Compass or use `mongosh`
   - Connect to `mongodb://localhost:27017`
   - Select database: `MachinaEar_IAM`
   - Select collection: `identities`
   - Find and delete the document with the matching username or email

3. **Delete User via API (If You Can Authenticate)**:
   
   If you have access to the account (can log in), you can delete it via the API:
   
   **Step 1: Login and get a token** (see section 3-4 above for full OAuth flow)
   
   **Step 2: Get your profile to find your identity ID**:
   ```powershell
   # Get profile (returns your identity ID)
   Invoke-WebRequest -Uri "http://localhost:8080/api/iam/identities/profile" -Method GET -Headers @{"Authorization"="Bearer {your_access_token}"}
   ```
   
   **Step 3: Delete the identity**:
   ```powershell
   # Delete identity by ID
   Invoke-WebRequest -Uri "http://localhost:8080/api/iam/identities/{identity_id}" -Method DELETE -Headers @{"Authorization"="Bearer {your_access_token}"}
   ```
   
   **Or using curl.exe**:
   ```powershell
   # Get profile
   curl.exe -X GET http://localhost:8080/api/iam/identities/profile -H "Authorization: Bearer {your_access_token}"
   
   # Delete identity
   curl.exe -X DELETE http://localhost:8080/api/iam/identities/{identity_id} -H "Authorization: Bearer {your_access_token}"
   ```

## Stopping the Server

Press `Ctrl+C` in the terminal where WildFly is running

## Useful URLs

- **API Base**: http://localhost:8080/api/iam
- **WildFly Management**: http://localhost:9990/management
- **Server Logs**: `target/server/standalone/log/server.log`

## Testing the IAM Web Interface

### Access the Web Pages

1. **Registration Page**:
   - Direct access: `http://localhost:8080/api/iam/register/authorize?client_id=testclient&redirect_uri=http://localhost:8080/api/iam/`
   - Note: You need to create a tenant first (see below)

2. **Login Page**:
   - Direct access: `http://localhost:8080/api/iam/authorize?response_type=code&client_id=testclient&state=teststate123&scope=resource.read resource.write&redirect_uri=http://localhost:8080/api/iam/&code_challenge=test_challenge&code_challenge_method=S256`
   - Note: You need to create a tenant first (see below)

3. **API Info Page**:
   - Direct access: `http://localhost:8080/api/iam/`

### Setting Up a Test Tenant (OAuth Client)

For OAuth flows to work, you need to create a tenant in MongoDB. You can do this using MongoDB shell or a MongoDB client:

```javascript
// Connect to MongoDB
use MachinaEar_IAM

// Create a test tenant
db.tenants.insertOne({
    _id: "test-tenant-1",
    tenant_name: "testclient",
    tenant_secret: "test-secret",
    redirect_uri: "http://localhost:8080/api/iam/",
    required_scopes: "resource.read resource.write",
    supported_grant_types: "authorization_code",
    allowed_roles: 1
})
```

**Or using PowerShell/MongoDB Compass:**
- Connect to MongoDB (usually `mongodb://localhost:27017`)
- Select database: `MachinaEar_IAM`
- Create a collection: `tenants` (if it doesn't exist)
- Insert a document with the above structure

### Testing the Registration Flow

1. **Register a new user**:
   - Go to: `http://localhost:8080/api/iam/register/authorize?client_id=testclient&redirect_uri=http://localhost:8080/api/iam/`
   - Fill in username, email, and password
   - Submit the form
   - You'll be redirected to the activation page

2. **Activate the account**:
   - Check server logs for the activation code: `target/server/standalone/log/server.log`
   - Look for: `"Registration successful for user: <username> (email: <email>). Activation code: <code>"`
   - Enter the activation code on the activation page
   - Upon success, you'll see a success message with a link to log in

3. **Log in**:
   - Use the login link or go to: `http://localhost:8080/api/iam/authorize?response_type=code&client_id=testclient&state=teststate123&scope=resource.read resource.write&redirect_uri=http://localhost:8080/api/iam/&code_challenge=test_challenge&code_challenge_method=S256`
   - Enter your username and password
   - Upon successful login, you'll be redirected with an authorization code

### Quick Test URLs

**Registration:**
```
http://localhost:8080/api/iam/register/authorize?client_id=testclient&redirect_uri=http://localhost:8080/api/iam/
```

**Login:**
```
http://localhost:8080/api/iam/authorize?response_type=code&client_id=testclient&state=test&scope=resource.read resource.write&redirect_uri=http://localhost:8080/api/iam/&code_challenge=test_challenge&code_challenge_method=S256
```

**Note**: Replace `testclient` with your actual tenant name, and update `redirect_uri`, `code_challenge`, and other OAuth parameters as needed for your use case.




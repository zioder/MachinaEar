# machinaear
MachinaEar is an intelligent predictive maintenance system that transforms low-cost vibration and sound sensors into powerful diagnostic tools for small-scale industrial machinery. By “listening” to motors, bearings, and moving parts, MachinaEar can detect early signs of failure before breakdowns occur.


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
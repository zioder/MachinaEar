# Scope Statement – MachinaEar

## Justification

### Context of the Project

MachinaEar is a predictive maintenance solution designed for small-scale industrial environments. It leverages real-time vibration and acoustic data from machines—such as Server Rooms,3D
printers, pumps, and motors—to detect early signs of failures or problems. By analyzing this data on the edge and delivering instant alerts via a mobile-optimized interface, MachinaEar
empowers users to intervene as soons as potential failures occur, minimizing downtime and repair costs.

### Problematic

Small workshops, makerspaces, academic labs, and hobbyist facilities often operate without access to enterprise-grade predictive maintenance systems due to cost and complexity. As a result,
mechanical issues are typically discovered only after catastrophic failure, leading to unplanned downtime, damaged components, and lost productivity.

### Ambitions

MachinaEar aims to democratize predictive maintenance by offering an affordable, easy-to-deploy system that continuously monitors machine health, identifies anomalies in real time, and
delivers actionable insights directly to users’ mobile devices—enabling proactive, data-driven maintenance decisions.

---

## Scope Description

### In Scope

- Continuous 24/7 real-time monitoring of small industrial machines (e.g., 3D printers, Routers, Servers, electric motors, pumps).
- On-device (edge) anomaly detection using AI models (TensorFlow Lite) running on Raspberry Pi 5.
- Real-time analysis of vibration and sound sensor data to identify deviations from normal operational patterns.
- Instant push notifications and maintenance recommendations delivered via a **Progressive Web App (PWA)** that functions seamlessly across mobile, tablet, and desktop platforms (iOS,
  Android, Windows, macOS, Linux).
- Cloud-based model retraining pipeline using MLflow and CI/CD (GitHub Actions) to improve detection accuracy over time.
- User-friendly dashboard within the PWA for monitoring machine status, viewing alerts, and accessing historical trends.
- Hardware kit including calibrated vibration/acoustic sensors and pre-configured Raspberry Pi 5 units.

### Out of Scope

- Monitoring of large industrial machinery (e.g., turbines, heavy presses).
- On-premise cloud infrastructure deployment (cloud services are managed via partner providers).
- Voice-based alerts or SMS fallback (initially limited to PWA notifications).

---

## Target Users

- Small manufacturers and micro-factories
- Makerspaces and innovation labs
- Technical training institutions and vocational schools
- Hobbyist engineers and advanced DIY workshops

---

## Business Model

### Customer Segments

- Small manufacturers and independent workshops
- Makerspaces and educational/training institutes
- IoT and maintenance-as-a-service providers

### Value Proposition

- Affordable, plug-and-play predictive maintenance for non-industrial settings
- Real-time machine health insights with instant mobile alerts
- Scalable architecture supporting diverse machine types
- Continuous learning via cloud-retrained AI models

### Channels

- Online IoT marketplaces (e.g., Seeed Studio, SparkFun)
- Direct e-commerce via MachinaEar website
- Strategic partnerships with 3D printer/CNC equipment vendors
- Distribution through makerspace networks and technical education programs

### Customer Relationships

- In-app support and regular PWA updates
- Community-driven feedback loop for model improvement
- Custom calibration services for specialized machines

### Revenue Streams

- One-time sales of sensor + Raspberry Pi hardware kits streamlined into a single device
- Tiered subscription for cloud analytics, model retraining, and advanced diagnostics
- Optional paid support for installation, onboarding, and maintenance

### Key Resources

- Vibration and acoustic IoT sensors
- Edge AI models (TensorFlow Lite)
- Cloud infrastructure (for MLOps and data aggregation)
- Progressive Web App (PWA) dashboard and notification system
- CI/CD pipeline (GitHub Actions + MLflow)

### Key Activities

- Development and validation of anomaly detection AI models
- Integration of sensors with Raspberry Pi edge devices
- PWA development (React-based, responsive, installable on all platforms)
- Cloud pipeline for data collection, labeling, and model retraining
- User testing and feedback integration with target communities

### Key Partnerships

- Sensor and Raspberry Pi hardware suppliers
- Cloud platform providers (e.g., AWS, Google Cloud)
- Makerspaces, Fab Labs, and technical training institutions

### Cost Structure

- R&D: AI/ML model development, IoT integration, and edge optimization
- Software development: PWA, backend services, mobile compatibility
- Cloud infrastructure and MLOps tooling
- Marketing, community engagement, and customer support

---

## Constraints

### Technical Stack

- **Server Side**: Jakarta EE
- **Database**: MongoDB
- **Application Server**: WildFly
- **Client Side**: React-based **Progressive Web App (PWA)**—fully responsive and installable on any device (mobile, tablet, desktop; iOS, Android, and all major browsers)
- **IoT Edge**: Raspberry Pi 5 with vibration and sound sensors
- **CI/CD & MLOps**: GitHub Actions for deployment; MLflow for experiment tracking and model versioning

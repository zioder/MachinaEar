package MachinaEar.devices.controllers.services;

import MachinaEar.devices.controllers.repositories.DeviceRepository;
import MachinaEar.devices.entities.Device;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Instant;
import java.util.logging.Logger;

@ApplicationScoped
public class MqttService {

    private static final Logger LOGGER = Logger.getLogger(MqttService.class.getName());
    
    private static final String BROKER_URL = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");
    private static final String CLIENT_ID = "machinaear-backend-" + System.currentTimeMillis();
    private static final double ANOMALY_THRESHOLD = 0.05;
    
    @Inject
    DeviceRepository deviceRepository;
    
    @Inject
    DeviceWebSocketEndpoint webSocketEndpoint;
    
    private MqttClient mqttClient;
    
    @PostConstruct
    public void init() {
        try {
            LOGGER.info("Initializing MQTT Service with broker: " + BROKER_URL);
            
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            
            // Set username/password if provided via environment variables
            String username = System.getenv("MQTT_USERNAME");
            String password = System.getenv("MQTT_PASSWORD");
            if (username != null && !username.isEmpty()) {
                options.setUserName(username);
                if (password != null) {
                    options.setPassword(password.toCharArray());
                }
            }
            
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    LOGGER.warning("MQTT connection lost: " + cause.getMessage());
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    handleMessage(topic, message);
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriptions
                }
            });
            
            mqttClient.connect(options);
            LOGGER.info("Connected to MQTT broker");
            
            // Subscribe to device topics
            mqttClient.subscribe("devices/+/anomaly", 1);
            mqttClient.subscribe("devices/+/status", 1);
            LOGGER.info("Subscribed to device topics");
            
        } catch (MqttException e) {
            LOGGER.severe("Failed to initialize MQTT service: " + e.getMessage());
            throw new RuntimeException("MQTT initialization failed", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                LOGGER.info("MQTT client disconnected");
            } catch (MqttException e) {
                LOGGER.warning("Error disconnecting MQTT client: " + e.getMessage());
            }
        }
    }
    
    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            LOGGER.info("Received MQTT message on topic " + topic + ": " + payload);
            
            // Extract device ID from topic (e.g., "devices/12345/anomaly" -> "12345")
            String[] topicParts = topic.split("/");
            if (topicParts.length < 3) {
                LOGGER.warning("Invalid topic format: " + topic);
                return;
            }
            
            String deviceId = topicParts[1];
            String messageType = topicParts[2];
            
            var deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                LOGGER.warning("Device not found: " + deviceId);
                return;
            }
            
            Device device = deviceOpt.get();
            
            if ("anomaly".equals(messageType)) {
                handleAnomalyMessage(device, payload);
            } else if ("status".equals(messageType)) {
                handleStatusMessage(device, payload);
            }
            
        } catch (Exception e) {
            LOGGER.severe("Error handling MQTT message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAnomalyMessage(Device device, String payload) {
        try {
            // Parse JSON payload: {"score": 0.073, "timestamp": "..."}
            // For simplicity, extract score using basic parsing (in production, use JSON library)
            double score = extractScore(payload);
            
            device.setAnomalyScore(score);
            device.setLastAnomalyDetection(Instant.now());
            
            // Update status based on threshold
            if (score > ANOMALY_THRESHOLD) {
                device.setStatus("abnormal");
                LOGGER.warning("Anomaly detected on device " + device.getId() + ": score=" + score);
            } else {
                device.setStatus("normal");
            }
            
            device.setIsOnline(true);
            device.setLastHeartbeat(Instant.now());
            device.touch();
            
            deviceRepository.update(device);
            
            // Broadcast to WebSocket clients
            webSocketEndpoint.broadcastDeviceUpdate(device);
            
        } catch (Exception e) {
            LOGGER.severe("Error handling anomaly message: " + e.getMessage());
        }
    }
    
    private void handleStatusMessage(Device device, String payload) {
        try {
            // Heartbeat message - just update online status
            device.setIsOnline(true);
            device.setLastHeartbeat(Instant.now());
            device.touch();
            
            deviceRepository.update(device);
            
            // Broadcast to WebSocket clients
            webSocketEndpoint.broadcastDeviceUpdate(device);
            
        } catch (Exception e) {
            LOGGER.severe("Error handling status message: " + e.getMessage());
        }
    }
    
    private double extractScore(String payload) {
        // Simple extraction of score from JSON like {"score": 0.073, ...}
        // In production, use jakarta.json or Jackson
        try {
            int scoreIdx = payload.indexOf("\"score\"");
            if (scoreIdx == -1) return 0.0;
            
            int colonIdx = payload.indexOf(":", scoreIdx);
            int commaIdx = payload.indexOf(",", colonIdx);
            if (commaIdx == -1) commaIdx = payload.indexOf("}", colonIdx);
            
            String scoreStr = payload.substring(colonIdx + 1, commaIdx).trim();
            return Double.parseDouble(scoreStr);
        } catch (Exception e) {
            LOGGER.warning("Failed to parse score from payload: " + payload);
            return 0.0;
        }
    }
}

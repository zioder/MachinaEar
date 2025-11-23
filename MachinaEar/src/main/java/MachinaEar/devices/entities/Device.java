package MachinaEar.devices.entities;

import MachinaEar.iam.entities.RootEntity;
import org.bson.types.ObjectId;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import MachinaEar.iam.json.ObjectIdAdapter;

import java.time.Instant;

public class Device extends RootEntity {
    private String name;
    private String type; // e.g., "Mobile", "Desktop", "IoT"
    @JsonbTypeAdapter(ObjectIdAdapter.class)
    private ObjectId identityId; // Owner of the device
    private String status; // "normal", "abnormal", "offline"
    private Instant lastHeartbeat; // Last time device checked in
    private Double temperature; // Device temperature (for monitoring)
    private Double cpuUsage; // CPU usage percentage
    private Double memoryUsage; // Memory usage percentage
    private String lastError; // Last error message if any

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ObjectId getIdentityId() { return identityId; }
    public void setIdentityId(ObjectId identityId) { this.identityId = identityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}

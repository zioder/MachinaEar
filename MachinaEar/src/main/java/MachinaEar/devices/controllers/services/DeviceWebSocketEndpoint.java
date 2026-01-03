package MachinaEar.devices.controllers.services;

import MachinaEar.devices.entities.Device;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
public class DeviceWebSocketEndpoint {
    private static final Logger LOGGER = Logger.getLogger(DeviceWebSocketEndpoint.class.getName());

    public void broadcastDeviceUpdate(Device device) {
        LOGGER.info("Stub: Broadcasting update for device " + device.getId());
        // TODO: Implement real WebSocket broadcasting
    }
}

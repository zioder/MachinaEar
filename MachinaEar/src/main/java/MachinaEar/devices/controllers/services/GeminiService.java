package MachinaEar.devices.controllers.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import MachinaEar.devices.entities.Device;

@ApplicationScoped
public class GeminiService {

    private static final String API_KEY = "AIzaSyBjPmZFkkjEwKxYex5M-cc5WJejjUAtKKk";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
            + API_KEY;

    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String chat(String userMessage, List<Device> devices) {
        try {
            // Préparer le contexte avec les données des devices
            String context = buildDeviceContext(devices);

            // Construire le prompt avec le contexte
            String prompt = buildPrompt(context, userMessage);

            // Construire la requête JSON pour l'API Gemini
            JsonObject requestBody = buildGeminiRequest(prompt);

            // Envoyer la requête à l'API Gemini
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Erreur lors de la communication avec l'API Gemini: " + response.body();
            }

            // Parser la réponse
            return parseGeminiResponse(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors du traitement de votre demande: " + e.getMessage();
        }
    }

    private String buildDeviceContext(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return "Aucun device trouvé.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== DONNÉES DES DEVICES ===\n\n");
        context.append("Nombre total de devices: ").append(devices.size()).append("\n\n");

        int normalCount = 0;
        int abnormalCount = 0;
        int offlineCount = 0;

        for (Device device : devices) {
            context.append("Device: ").append(device.getName()).append("\n");
            context.append("  - Type: ").append(device.getType()).append("\n");
            context.append("  - Statut: ").append(device.getStatus()).append("\n");
            context.append("  - En ligne: ").append(device.getIsOnline() != null ? device.getIsOnline() : "inconnu")
                    .append("\n");

            if (device.getTemperature() != null) {
                context.append("  - Température: ").append(device.getTemperature()).append("°C\n");
            }
            if (device.getCpuUsage() != null) {
                context.append("  - Utilisation CPU: ").append(device.getCpuUsage()).append("%\n");
            }
            if (device.getMemoryUsage() != null) {
                context.append("  - Utilisation Mémoire: ").append(device.getMemoryUsage()).append("%\n");
            }
            if (device.getAnomalyScore() != null) {
                context.append("  - Score d'anomalie: ").append(device.getAnomalyScore()).append("\n");
            }
            if (device.getLastError() != null && !device.getLastError().isEmpty()) {
                context.append("  - Dernière erreur: ").append(device.getLastError()).append("\n");
            }
            if (device.getLastHeartbeat() != null) {
                context.append("  - Dernier heartbeat: ").append(device.getLastHeartbeat()).append("\n");
            }
            if (device.getLastAnomalyDetection() != null) {
                context.append("  - Dernière détection d'anomalie: ").append(device.getLastAnomalyDetection())
                        .append("\n");
            }
            context.append("\n");

            // Compter les statuts
            String status = device.getStatus();
            if ("normal".equals(status))
                normalCount++;
            else if ("abnormal".equals(status))
                abnormalCount++;
            else if ("offline".equals(status))
                offlineCount++;
        }

        context.append("=== RÉSUMÉ ===\n");
        context.append("Devices normaux: ").append(normalCount).append("\n");
        context.append("Devices anormaux: ").append(abnormalCount).append("\n");
        context.append("Devices hors ligne: ").append(offlineCount).append("\n");

        return context.toString();
    }

    private String buildPrompt(String deviceContext, String userMessage) {
        return "Tu es un assistant IA spécialisé dans la surveillance et l'analyse de machines IoT pour le système MachinaEar.\n\n"
                +
                "Voici les données actuelles des devices:\n\n" +
                deviceContext + "\n\n" +
                "Question de l'utilisateur: " + userMessage + "\n\n" +
                "Réponds de manière concise et professionnelle en français. " +
                "Si l'utilisateur demande un résumé, fournis une vue d'ensemble claire. " +
                "Si il demande des détails sur un device spécifique, fournis les informations pertinentes. " +
                "Si il demande l'historique, analyse les données temporelles disponibles (lastHeartbeat, lastAnomalyDetection). "
                +
                "Sois factuel et base-toi uniquement sur les données fournies.";
    }

    private JsonObject buildGeminiRequest(String prompt) {
        JsonObjectBuilder contentBuilder = Json.createObjectBuilder()
                .add("parts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("text", prompt)));

        JsonArrayBuilder contentsArray = Json.createArrayBuilder()
                .add(contentBuilder);

        return Json.createObjectBuilder()
                .add("contents", contentsArray)
                .add("generationConfig", Json.createObjectBuilder()
                        .add("temperature", 0.7)
                        .add("maxOutputTokens", 1024))
                .build();
    }

    private String parseGeminiResponse(String responseBody) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(responseBody));
            JsonObject jsonResponse = jsonReader.readObject();
            jsonReader.close();

            JsonArray candidates = jsonResponse.getJsonArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JsonObject firstCandidate = candidates.getJsonObject(0);
                JsonObject content = firstCandidate.getJsonObject("content");
                JsonArray parts = content.getJsonArray("parts");
                if (parts != null && !parts.isEmpty()) {
                    JsonObject firstPart = parts.getJsonObject(0);
                    return firstPart.getString("text", "Aucune réponse générée.");
                }
            }

            return "Erreur: impossible de parser la réponse de Gemini.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de l'analyse de la réponse: " + e.getMessage();
        }
    }
}

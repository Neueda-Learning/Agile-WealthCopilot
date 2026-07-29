package com.wealthcopilot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("!stub")
public class DeepSeekLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeepSeekLlmClient(RestClient.Builder builder, LlmProperties properties, ObjectMapper objectMapper) {
        this(createRestClient(builder, properties), properties, objectMapper);
    }

    DeepSeekLlmClient(RestClient restClient, LlmProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private static RestClient createRestClient(RestClient.Builder builder, LlmProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public LlmResult complete(List<LlmMessage> messages, List<LlmToolDefinition> tools, boolean jsonMode) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmClientException("DeepSeek API key is not configured");
        }

        ObjectNode body = buildRequestBody(messages, tools, jsonMode);
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw new LlmClientException(
                    "DeepSeek request failed with status " + exception.getStatusCode().value(),
                    exception);
        } catch (RestClientException exception) {
            throw new LlmClientException("DeepSeek request failed", exception);
        }

        return parseResponse(response);
    }

    private ObjectNode buildRequestBody(
            List<LlmMessage> messages,
            List<LlmToolDefinition> tools,
            boolean jsonMode) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.0);

        ArrayNode messageArray = body.putArray("messages");
        for (LlmMessage message : messages) {
            messageArray.add(toMessageNode(message));
        }

        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolArray = body.putArray("tools");
            for (LlmToolDefinition tool : tools) {
                ObjectNode toolNode = toolArray.addObject();
                toolNode.put("type", "function");
                ObjectNode functionNode = toolNode.putObject("function");
                functionNode.put("name", tool.name());
                functionNode.put("description", tool.description());
                functionNode.set("parameters", readSchema(tool));
            }
        }

        if (jsonMode) {
            body.putObject("response_format").put("type", "json_object");
        }
        return body;
    }

    private ObjectNode toMessageNode(LlmMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", message.role().name().toLowerCase(Locale.ROOT));
        node.put("content", message.content() == null ? "" : message.content());
        if (message.role() == LlmMessage.Role.ASSISTANT && !message.toolCalls().isEmpty()) {
            ArrayNode calls = node.putArray("tool_calls");
            for (LlmToolCall call : message.toolCalls()) {
                ObjectNode callNode = calls.addObject();
                callNode.put("id", call.id());
                callNode.put("type", "function");
                ObjectNode functionNode = callNode.putObject("function");
                functionNode.put("name", call.name());
                functionNode.put("arguments", call.argumentsJson());
            }
        }
        if (message.role() == LlmMessage.Role.TOOL) {
            node.put("tool_call_id", message.toolCallId());
        }
        return node;
    }

    private JsonNode readSchema(LlmToolDefinition tool) {
        try {
            return objectMapper.readTree(tool.parametersSchemaJson());
        } catch (Exception exception) {
            throw new LlmClientException(
                    "Invalid parameter schema for tool " + tool.name(), exception);
        }
    }

    private LlmResult parseResponse(JsonNode response) {
        JsonNode message = response == null
                ? null
                : response.path("choices").path(0).path("message");
        if (message == null || message.isMissingNode()) {
            throw new LlmClientException("DeepSeek returned an empty response");
        }

        String content = message.path("content").isTextual()
                ? message.path("content").asText()
                : null;

        List<LlmToolCall> toolCalls = new ArrayList<>();
        JsonNode callsNode = message.path("tool_calls");
        if (callsNode.isArray()) {
            for (JsonNode callNode : callsNode) {
                JsonNode functionNode = callNode.path("function");
                toolCalls.add(new LlmToolCall(
                        callNode.path("id").asText(),
                        functionNode.path("name").asText(),
                        functionNode.path("arguments").asText("{}")));
            }
        }
        return new LlmResult(content, List.copyOf(toolCalls));
    }
}

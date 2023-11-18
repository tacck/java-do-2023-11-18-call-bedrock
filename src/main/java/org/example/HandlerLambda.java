package org.example;

import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

public class HandlerLambda implements RequestHandler<MessageRecord, String> {
    private final BedrockRuntimeClient bedrockRuntimeClient;

    public HandlerLambda() {
        bedrockRuntimeClient = BedrockRuntimeClient.builder().region(Region.US_EAST_1).build();
    }

    @Override
    public String handleRequest(MessageRecord event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("String found: " + event.message());

        return sendRequest(event.message());
    }

    public String sendRequest(String prompt) {
        String body = """
                    {
                      "prompt": "%s",
                      "maxTokens": 200,
                      "temperature": 0,
                      "topP": 1,
                      "stopSequences": [],
                      "countPenalty": { "scale": 0 },
                      "presencePenalty": { "scale": 0 },
                      "frequencyPenalty": { "scale": 0 }
                    }
                """.formatted(prompt);

        InvokeModelResponse response = bedrockRuntimeClient.invokeModel(InvokeModelRequest.builder()
            .modelId("ai21.j2-ultra-v1")
            .contentType("application/json")
            .accept("*/*")
            .body(SdkBytes.fromString(body,  StandardCharsets.UTF_8))
            .build()
        );
        String responseBody = new String(response.body().asByteArray());

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(responseBody);
            String results = root.get("completions").get(0).get("data").get("text").toString();
            System.out.println("results: " + results);
            return results;
        } catch (Exception e) {
            System.out.println(e);
        }

        bedrockRuntimeClient.close();
        return "no answer";
    }
}

record MessageRecord(String message) {}
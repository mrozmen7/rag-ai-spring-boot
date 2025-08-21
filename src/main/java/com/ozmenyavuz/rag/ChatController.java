package com.ozmenyavuz.rag;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        // RAG search settings (can be fine-tuned if needed)
        var req = SearchRequest.builder()
                .topK(10)
                .similarityThreshold(0.10); // lower the threshold a bit (0.0–1.0)

        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();

        this.vectorStore = vectorStore;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("q") String question) {

        String rewritten = question;
        // If question contains "introduction", rewrite it for better retrieval
        if (question.toLowerCase().contains("introduction")) {
            rewritten = "Answer strictly based on the INTRODUCTION section only. Question: " + question;
        }

        String system = """
                You are a helpful assistant.
                Use ONLY the provided CONTEXT to answer the USER question.
                If the context is insufficient, reply exactly with "I don’t know.".
                Be concise, accurate, and professional.
                Always answer in English.
                """;
        return chatClient
                .prompt()
                .system(system)
                .user(rewritten)
                .call()
                .content();
    }

    // Debug: See raw chunks coming from the vector store
    @GetMapping("/search")
    public String search(@RequestParam("q") String q) {
        SearchRequest req = SearchRequest.builder()
                .query(q)                  // search query
                .topK(6)                   // number of results to return
                .similarityThreshold(0.0) // optional threshold (0.0–1.0)
                .build();

        var docs = vectorStore.similaritySearch(req);

        return docs.stream()
                .map(d -> String.valueOf(d.getText()))
                .map(s -> s.length() > 240 ? s.substring(0, 240) + "…" : s)
                .collect(Collectors.joining("\n---\n"));
    }
}
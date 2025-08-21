package com.ozmenyavuz.rag;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Getter
@Setter
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;

    @Value("classpath:/docs/article_thebeatact2024.pdf")
    private Resource pdf;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        try {
            // Read PDF
            var reader = new PagePdfDocumentReader(pdf);

            // Chunk settings: longer chunks + less overlap
            var splitter = TokenTextSplitter.builder()
                    .withChunkSize(800)         // length of each chunk
                    .withMinChunkSizeChars(200)
                    .withKeepSeparator(true)// minimum chunk length (acts like overlap handling)
                    .build(); // (chunkSize, overlap)

            vectorStore.accept(splitter.apply(reader.get()));

            log.info("VectorStore loaded, first chunk preview: {}", pdf.getFilename());
        } catch (Exception e) {
            log.error("Ingestion failed!", e);
        }
    }

    // Basit metin temizleyici
    private static String clean(String s) {
        if (s == null) return "";
        return s
                .replace("\u00AD", "")
                .replaceAll("(?m)(\\S)-\\s*\\n\\s*(\\S)", "$1$2")
                .replaceAll("\\s*\\n\\s*", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }
}
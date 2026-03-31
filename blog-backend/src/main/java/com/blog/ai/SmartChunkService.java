package com.blog.ai;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能文档分块服务
 * 支持多种分块策略：按段落、按句子、按语义、滑动窗口
 */
@Slf4j
@Service
public class SmartChunkService {

    @Value("${app.ai.rag.chunk-size:500}")
    private int defaultChunkSize;

    @Value("${app.ai.rag.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${app.ai.rag.chunk-strategy:semantic}")
    private String chunkStrategy;

    private SentenceDetectorME sentenceDetector;

    @PostConstruct
    public void init() {
        try {
            // 加载 OpenNLP 句子检测模型
            ClassPathResource resource = new ClassPathResource("opennlp/en-sent.bin");
            if (resource.exists()) {
                try (InputStream modelIn = resource.getInputStream()) {
                    SentenceModel model = new SentenceModel(modelIn);
                    sentenceDetector = new SentenceDetectorME(model);
                }
            }
        } catch (Exception e) {
            log.warn("句子检测模型加载失败，将使用简单分句: {}", e.getMessage());
        }
    }

    /**
     * 文档分块
     */
    public List<Chunk> chunk(String content, String docType) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return switch (chunkStrategy.toLowerCase()) {
            case "paragraph" -> chunkByParagraph(content);
            case "sentence" -> chunkBySentence(content);
            case "semantic" -> chunkBySemantic(content, docType);
            case "fixed" -> chunkByFixedSize(content);
            default -> chunkBySemantic(content, docType);
        };
    }

    /**
     * 按段落分块
     */
    private List<Chunk> chunkByParagraph(String content) {
        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\s*\n");
        
        int index = 0;
        int position = 0;
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.length() < 10) continue; // 过滤太短的段落
            
            Chunk chunk = new Chunk();
            chunk.setIndex(index++);
            chunk.setContent(trimmed);
            chunk.setStartPos(position);
            chunk.setEndPos(position + paragraph.length());
            chunk.setType("paragraph");
            chunks.add(chunk);
            
            position += paragraph.length() + 2;
        }
        
        return chunks;
    }

    /**
     * 按句子分块
     */
    private List<Chunk> chunkBySentence(String content) {
        List<Chunk> chunks = new ArrayList<>();
        String[] sentences;
        
        if (sentenceDetector != null) {
            sentences = sentenceDetector.sentDetect(content);
        } else {
            // 简单分句
            sentences = content.split("(?<=[。！？.!?])\\s*");
        }
        
        StringBuilder currentChunk = new StringBuilder();
        int index = 0;
        int startPos = 0;
        int currentPos = 0;
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;
            
            if (currentChunk.length() + trimmed.length() > defaultChunkSize && currentChunk.length() > 0) {
                // 保存当前块
                Chunk chunk = new Chunk();
                chunk.setIndex(index++);
                chunk.setContent(currentChunk.toString().trim());
                chunk.setStartPos(startPos);
                chunk.setEndPos(currentPos);
                chunk.setType("sentence");
                chunks.add(chunk);
                
                // 重叠处理
                String overlap = getOverlap(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlap);
                startPos = currentPos - overlap.length();
            }
            
            currentChunk.append(trimmed).append(" ");
            currentPos += trimmed.length() + 1;
        }
        
        // 添加最后一块
        if (currentChunk.length() > 0) {
            Chunk chunk = new Chunk();
            chunk.setIndex(index);
            chunk.setContent(currentChunk.toString().trim());
            chunk.setStartPos(startPos);
            chunk.setEndPos(currentPos);
            chunk.setType("sentence");
            chunks.add(chunk);
        }
        
        return chunks;
    }

    /**
     * 按语义分块（智能分块）
     */
    private List<Chunk> chunkBySemantic(String content, String docType) {
        List<Chunk> chunks = new ArrayList<>();
        
        // 根据文档类型选择不同策略
        if ("markdown".equalsIgnoreCase(docType) || content.contains("#")) {
            chunks = chunkByMarkdownHeaders(content);
        } else if ("code".equalsIgnoreCase(docType) || content.contains("```")) {
            chunks = chunkByCodeBlocks(content);
        } else {
            chunks = chunkBySentence(content);
        }
        
        // 添加元数据
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            chunk.setMetadata(extractMetadata(chunk.getContent()));
            
            // 添加上下文信息
            if (i > 0) {
                chunk.setPrevChunk(chunks.get(i - 1).getContent().substring(0, 
                    Math.min(100, chunks.get(i - 1).getContent().length())));
            }
            if (i < chunks.size() - 1) {
                chunk.setNextChunk(chunks.get(i + 1).getContent().substring(0, 
                    Math.min(100, chunks.get(i + 1).getContent().length())));
            }
        }
        
        return chunks;
    }

    /**
     * 按 Markdown 标题分块
     */
    private List<Chunk> chunkByMarkdownHeaders(String content) {
        List<Chunk> chunks = new ArrayList<>();
        
        // 匹配 Markdown 标题
        Pattern pattern = Pattern.compile("^(#{1,6}\\s+.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        
        List<Integer> headerPositions = new ArrayList<>();
        headerPositions.add(0);
        
        while (matcher.find()) {
            headerPositions.add(matcher.start());
        }
        headerPositions.add(content.length());
        
        int index = 0;
        for (int i = 0; i < headerPositions.size() - 1; i++) {
            int start = headerPositions.get(i);
            int end = headerPositions.get(i + 1);
            String section = content.substring(start, end).trim();
            
            if (section.length() < 10) continue;
            
            Chunk chunk = new Chunk();
            chunk.setIndex(index++);
            chunk.setContent(section);
            chunk.setStartPos(start);
            chunk.setEndPos(end);
            chunk.setType("markdown_section");
            chunks.add(chunk);
        }
        
        return chunks;
    }

    /**
     * 按代码块分块
     */
    private List<Chunk> chunkByCodeBlocks(String content) {
        List<Chunk> chunks = new ArrayList<>();
        
        // 匹配代码块
        Pattern pattern = Pattern.compile("```[\\w]*\\n[\\s\\S]*?```");
        Matcher matcher = pattern.matcher(content);
        
        int lastEnd = 0;
        int index = 0;
        
        while (matcher.find()) {
            // 添加代码块前的文本
            if (matcher.start() > lastEnd) {
                String textChunk = content.substring(lastEnd, matcher.start()).trim();
                if (textChunk.length() >= 10) {
                    Chunk chunk = new Chunk();
                    chunk.setIndex(index++);
                    chunk.setContent(textChunk);
                    chunk.setStartPos(lastEnd);
                    chunk.setEndPos(matcher.start());
                    chunk.setType("text");
                    chunks.add(chunk);
                }
            }
            
            // 添加代码块
            String codeChunk = matcher.group();
            Chunk chunk = new Chunk();
            chunk.setIndex(index++);
            chunk.setContent(codeChunk);
            chunk.setStartPos(matcher.start());
            chunk.setEndPos(matcher.end());
            chunk.setType("code");
            chunks.add(chunk);
            
            lastEnd = matcher.end();
        }
        
        // 添加剩余文本
        if (lastEnd < content.length()) {
            String textChunk = content.substring(lastEnd).trim();
            if (textChunk.length() >= 10) {
                Chunk chunk = new Chunk();
                chunk.setIndex(index);
                chunk.setContent(textChunk);
                chunk.setStartPos(lastEnd);
                chunk.setEndPos(content.length());
                chunk.setType("text");
                chunks.add(chunk);
            }
        }
        
        return chunks;
    }

    /**
     * 固定大小分块
     */
    private List<Chunk> chunkByFixedSize(String content) {
        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        int position = 0;
        
        while (position < content.length()) {
            int end = Math.min(position + defaultChunkSize, content.length());
            
            // 尝试在句子边界截断
            if (end < content.length()) {
                int lastSentenceEnd = findLastSentenceEnd(content, position, end);
                if (lastSentenceEnd > position) {
                    end = lastSentenceEnd;
                }
            }
            
            String chunkContent = content.substring(position, end).trim();
            if (chunkContent.length() >= 10) {
                Chunk chunk = new Chunk();
                chunk.setIndex(index++);
                chunk.setContent(chunkContent);
                chunk.setStartPos(position);
                chunk.setEndPos(end);
                chunk.setType("fixed");
                chunks.add(chunk);
            }
            
            position = end - chunkOverlap;
            if (position <= end - defaultChunkSize) {
                position = end;
            }
        }
        
        return chunks;
    }

    /**
     * 查找最后一个句子结束位置
     */
    private int findLastSentenceEnd(String content, int start, int end) {
        String substring = content.substring(start, end);
        int lastPeriod = substring.lastIndexOf('。');
        int lastExclaim = substring.lastIndexOf('！');
        int lastQuestion = substring.lastIndexOf('？');
        int lastDot = substring.lastIndexOf('.');
        
        int max = Math.max(Math.max(lastPeriod, lastExclaim), Math.max(lastQuestion, lastDot));
        return max > 0 ? start + max + 1 : end;
    }

    /**
     * 获取重叠文本
     */
    private String getOverlap(String text, int overlapSize) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }

    /**
     * 提取元数据
     */
    private ChunkMetadata extractMetadata(String content) {
        ChunkMetadata metadata = new ChunkMetadata();
        
        // 提取关键词（简单实现）
        metadata.setKeywords(extractKeywords(content));
        
        // 检测内容类型
        metadata.setContentType(detectContentType(content));
        
        // 统计信息
        metadata.setCharCount(content.length());
        metadata.setWordCount(content.split("\\s+").length);
        
        return metadata;
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        List<String> keywords = new ArrayList<>();
        
        // 简单关键词提取：找出频率较高的词
        String[] words = content.toLowerCase().split("\\s+");
        java.util.Map<String, Integer> wordCount = new java.util.HashMap<>();
        
        for (String word : words) {
            word = word.replaceAll("[^\\w\\u4e00-\\u9fa5]", "");
            if (word.length() > 2) {
                wordCount.merge(word, 1, Integer::sum);
            }
        }
        
        wordCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(e -> keywords.add(e.getKey()));
        
        return keywords;
    }

    /**
     * 检测内容类型
     */
    private String detectContentType(String content) {
        if (content.contains("```")) return "code";
        if (content.matches("(?s).*#+\\s+.+")) return "markdown";
        if (content.matches("(?s).*\\|[-:]+\\|.*")) return "table";
        return "text";
    }

    // 内部类
    @lombok.Data
    public static class Chunk {
        private int index;
        private String content;
        private int startPos;
        private int endPos;
        private String type;
        private ChunkMetadata metadata;
        private String prevChunk;
        private String nextChunk;
    }

    @lombok.Data
    public static class ChunkMetadata {
        private List<String> keywords;
        private String contentType;
        private int charCount;
        private int wordCount;
    }
}

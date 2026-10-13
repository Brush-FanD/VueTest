package com.hkd.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯字符切块器：把长文切成带重叠的小块，供 embedding 使用。
 *
 * <p>先按目标长度滑窗，再尽量把切口对齐到段落 / 句号处，避免把一句话从中间劈开。</p>
 */
public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> split(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            return chunks;
        }
        int size = Math.max(100, chunkSize);
        int over = Math.max(0, Math.min(overlap, size / 2));
        int length = normalized.length();

        int start = 0;
        while (start < length) {
            int end = Math.min(length, start + size);
            int cut = end;
            if (end < length) {
                int min = start + size / 2;
                int breakAt = findBreak(normalized, min, end);
                if (breakAt > min) {
                    cut = breakAt;
                }
            }
            String chunk = normalized.substring(start, cut).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (cut >= length) {
                break;
            }
            // 回退 overlap 个字符形成重叠，既保留上下文又避免块与块之间断裂
            start = Math.max(start + 1, cut - over);
        }
        return chunks;
    }

    /** 在 (min, max] 内从后往前找最近的句子 / 段落边界 */
    private static int findBreak(String text, int min, int max) {
        for (int i = max; i > min; i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == ';') {
                return i;
            }
        }
        return max;
    }
}
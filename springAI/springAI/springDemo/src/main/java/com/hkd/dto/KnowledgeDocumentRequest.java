package com.hkd.dto;

import lombok.Data;

/** 知识库入库请求体 */
@Data
public class KnowledgeDocumentRequest {

    /** 资料标题，留空时用来源或“未命名资料” */
    private String title;

    /** 资料来源，比如文件名 / URL，用于回答时展示引用 */
    private String source;

    /** 资料正文（纯文本） */
    private String content;
}
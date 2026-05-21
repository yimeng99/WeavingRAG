package com.weaving.llm.rag.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "知识库切片管理", description = "知识库切片等管理接口")
@RestController
@RequestMapping("/v0/knowledge/chunk")
public class KnowledgeChunkController {


}
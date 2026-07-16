package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentRoleSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.knowledge.domain.KnowledgeSourceDocument;

import java.util.Collection;
import java.util.List;

/**
 * 知识源文档服务。
 */
public interface IKnowledgeSourceDocumentService extends IService<KnowledgeSourceDocument> {

    String saveDocument(KnowledgeDocumentSaveDTO dto);

    void saveDocumentRoles(KnowledgeDocumentRoleSaveDTO dto);

    List<String> listVisibleSourceDocumentIds(String tenantId, Collection<String> roleCodes);
}

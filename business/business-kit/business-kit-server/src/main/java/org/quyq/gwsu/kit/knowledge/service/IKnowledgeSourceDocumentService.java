package org.quyq.gwsu.kit.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentQueryDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentRoleSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.dto.KnowledgeDocumentSaveDTO;
import org.quyq.gwsu.kit.api.knowledge.vo.KnowledgeDocumentVO;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgeSourceDocument;

import java.util.Collection;
import java.util.List;

/**
 * 知识源文档服务。
 */
public interface IKnowledgeSourceDocumentService extends IService<KitKnowledgeSourceDocument> {

    String saveDocument(KnowledgeDocumentSaveDTO dto);

    void saveDocumentRoles(KnowledgeDocumentRoleSaveDTO dto);

    IPage<KnowledgeDocumentVO> pageDocuments(KnowledgeDocumentQueryDTO dto);

    KnowledgeDocumentVO getDocument(String documentId);

    void updateEnabled(String documentId, boolean enabled);

    void deleteDocument(String documentId);

    List<String> listVisibleSourceDocumentIds(Collection<String> roleCodes);
}

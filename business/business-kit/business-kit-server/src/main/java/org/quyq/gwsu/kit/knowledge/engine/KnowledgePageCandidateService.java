package org.quyq.gwsu.kit.knowledge.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.kit.config.properties.KnowledgeProperties;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePage;
import org.quyq.gwsu.kit.knowledge.domain.KitKnowledgePageVersion;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageMapper;
import org.quyq.gwsu.kit.knowledge.mapper.KnowledgePageVersionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Page 归属候选召回服务。
 */
@Component
@RequiredArgsConstructor
public class KnowledgePageCandidateService {

    private final KnowledgeProperties properties;

    private final KnowledgePageMapper pageMapper;

    private final KnowledgePageVersionMapper pageVersionMapper;

    public List<KnowledgePageCandidate> recall(GeneratedKnowledgePage incomingPage) {
        if (!StringUtils.hasText(incomingPage.title())) {
            return List.of();
        }
        List<KitKnowledgePage> pages = pageMapper.selectPage(
                Page.of(1, Math.max(properties.getPageMatchRecallSize(), properties.getPageMatchCandidateSize())),
                new LambdaQueryWrapper<KitKnowledgePage>()
                        .eq(KitKnowledgePage::getDeleted, false)
                        .isNotNull(KitKnowledgePage::getCurrentVersionId)
                        .orderByDesc(KitKnowledgePage::getModifyTime)
                        .orderByDesc(KitKnowledgePage::getCreateTime))
                .getRecords();
        if (CollectionUtils.isEmpty(pages)) {
            return List.of();
        }
        Set<String> incomingTerms = terms(incomingPage.title() + "\n" + incomingPage.markdownContent());
        List<KnowledgePageCandidate> candidates = new ArrayList<>();
        for (KitKnowledgePage page : pages) {
            KitKnowledgePageVersion version = pageVersionMapper.selectOne(new LambdaQueryWrapper<KitKnowledgePageVersion>()
                    .eq(KitKnowledgePageVersion::getId, page.getCurrentVersionId())
                    .eq(KitKnowledgePageVersion::getDeleted, false));
            if (Objects.isNull(version)) {
                continue;
            }
            String content = Objects.requireNonNullElse(version.getMarkdownContent(), "");
            Set<String> candidateTerms = terms(page.getTitle() + "\n" + content);
            double score = score(incomingPage.title(), page.getTitle(), incomingTerms, candidateTerms);
            if (score <= 0D) {
                continue;
            }
            candidates.add(new KnowledgePageCandidate(
                    page.getId(),
                    page.getTitle(),
                    excerpt(content),
                    score));
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(KnowledgePageCandidate::score).reversed())
                .limit(properties.getPageMatchCandidateSize())
                .toList();
    }

    private double score(String incomingTitle,
                         String candidateTitle,
                         Set<String> incomingTerms,
                         Set<String> candidateTerms) {
        double titleScore = normalized(incomingTitle).equals(normalized(candidateTitle)) ? 2.0D : 0D;
        if (normalized(candidateTitle).contains(normalized(incomingTitle))
                || normalized(incomingTitle).contains(normalized(candidateTitle))) {
            titleScore += 1.0D;
        }
        if (incomingTerms.isEmpty() || candidateTerms.isEmpty()) {
            return titleScore;
        }
        long overlap = incomingTerms.stream().filter(candidateTerms::contains).count();
        return titleScore + ((double) overlap / Math.max(1, Math.min(incomingTerms.size(), candidateTerms.size())));
    }

    private Set<String> terms(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        String normalized = normalized(text);
        String[] parts = normalized.split("[^\\p{IsHan}\\p{Alnum}]+");
        Set<String> terms = new LinkedHashSet<>();
        for (String part : parts) {
            if (part.length() >= 2) {
                terms.add(part);
            }
        }
        return terms;
    }

    private String normalized(String text) {
        return Objects.requireNonNullElse(text, "")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        int maxLength = 2000;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "\n...[已截断]";
    }
}

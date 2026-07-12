package org.quyq.gwsu.common.ai.skill;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;

import java.util.*;

public class InMemoryAgentSkillRepository implements AgentSkillRepository {

    private final Map<String, AgentSkill> skills = new LinkedHashMap<>();

    private final AgentSkillRepositoryInfo repositoryInfo;

    private final String source;

    private boolean writeable;

    public InMemoryAgentSkillRepository(String source, List<AgentSkill> skills, boolean writeable) {
        this.source = Objects.requireNonNullElse(source, "memory");
        this.writeable = writeable;
        this.repositoryInfo = new AgentSkillRepositoryInfo("memory", this.source, writeable);
        if (skills != null) {
            skills.forEach(skill -> this.skills.put(skill.getSkillId(), skill));
        }
    }

    @Override
    public AgentSkill getSkill(String skillId) {
        return skills.get(skillId);
    }

    @Override
    public List<String> getAllSkillNames() {
        return new ArrayList<>(skills.keySet());
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        return new ArrayList<>(skills.values());
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean overwrite) {
        if (!writeable || skills == null) {
            return false;
        }
        for (AgentSkill skill : skills) {
            String skillId = skill.getSkillId();
            if (!overwrite && this.skills.containsKey(skillId)) {
                continue;
            }
            this.skills.put(skillId, skill);
        }
        return true;
    }

    @Override
    public boolean delete(String skillId) {
        if (!writeable) {
            return false;
        }
        return skills.remove(skillId) != null;
    }

    @Override
    public boolean skillExists(String skillId) {
        return skills.containsKey(skillId);
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    @Override
    public boolean isWriteable() {
        return writeable;
    }
}

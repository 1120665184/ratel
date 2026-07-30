package org.quyq.gwsu.common.ai.agui.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@Data
@Accessors(chain = true)
public class CopilotKitInfo {

    private Map<String, Agents> agents = new HashMap<>();


    private Capabilities capabilities = Capabilities.builder()
            .threads(true)
            .generativeUi(true)
            .build();


    public CopilotKitInfo addAgent(Agents agent) {
        agents.put(agent.name(), agent);
        return this;
    }


    public record Agents(
            String name,
            String description
    ) {
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capabilities {
        public boolean threads;
        public boolean generativeUi;
    }

}

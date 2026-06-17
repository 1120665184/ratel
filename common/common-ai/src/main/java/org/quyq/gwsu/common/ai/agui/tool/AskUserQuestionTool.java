package org.quyq.gwsu.common.ai.agui.tool;


import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;

import java.util.List;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/8
 * @description 问用户问题工具
 */
@RequiredArgsConstructor
public class AskUserQuestionTool {

    @Tool(name = AIConstants.ToolName.ASK_USER_QUESTION, description = """
            Use this tool when you need to ask the user questions during execution. This allows you to:
            
            Gather user preferences or requirements
            Clarify ambiguous instructions
            Get decisions on implementation choices as you work
            Offer choices to the user about what direction to take.
            Usage notes:
            
            Users will always be able to select "Other" to provide custom text input
            Use multiSelect: true to allow multiple answers to be selected for a question
            If you recommend a specific option, make that the first option in the list and add "(Recommended)" at the end of the label
            
            Parameters:
            
            - questions (required): 1-4 questions to ask, each with:
                - question (required): The complete question to ask the user
                - header (required): Very short label (max 12 chars)
                - options (required): 2-4 options, each with label and description
                - multiSelect (required): Allow multiple selections (default false)
            
            - answers: User answers collected by the component
            - annotations: Optional per-question annotations with preview and notes
            
            """)
    public ToolResultBlock askUserQuestion(@ToolParam(name = "questions", description = "1-4 questions to ask")
                                           List<QuestionParam> questions) {
        throw new ToolSuspendException("等待用户作答");
    }


    public record QuestionParam(
            @ToolParam(name = "question", description = "The complete question to ask the user")
            String question,
            @ToolParam(name = "header", description = "Very short label (max 12 chars)")
            String header,
            @ToolParam(name = "options", description = "2-4 options, each with label and description")
            List<QuestionOption> options,
            @ToolParam(name = "multiSelect", description = "Allow multiple selections (default false)", required = false)
            Boolean multiSelect

    ) {
        public QuestionParam {
            multiSelect = Objects.nonNull(multiSelect) && multiSelect;
        }
    }

    public record QuestionOption(
            @ToolParam(name = "label", description = "标签")
            String label,
            @ToolParam(name = "description", description = "描述")
            String description

    ) {
    }

}

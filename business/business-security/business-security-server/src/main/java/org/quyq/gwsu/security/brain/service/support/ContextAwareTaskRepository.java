package org.quyq.gwsu.security.brain.service.support;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.subagent.task.BackgroundTask;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskRunSpec;
import io.agentscope.harness.agent.subagent.task.TaskStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 使用业务侧线程池承载异步子智能体任务，确保 Micrometer ThreadLocal 上下文可透传。
 */
public class ContextAwareTaskRepository implements TaskRepository {

    private final Map<String, BackgroundTask> tasks = new ConcurrentHashMap<>();

    private final ExecutorService executorService;

    public ContextAwareTaskRepository(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public BackgroundTask getTask(RuntimeContext rc, String sessionId, String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public BackgroundTask putTask(
            RuntimeContext rc,
            String taskId,
            String subAgentId,
            String sessionId,
            TaskRunSpec spec) {
        CompletableFuture<String> future;
        if (spec instanceof TaskRunSpec.LocalTaskRunSpec(Supplier<String> execution)) {
            future = CompletableFuture.supplyAsync(execution, executorService);
        } else if (spec instanceof TaskRunSpec.AdoptedTaskRunSpec(CompletableFuture<String> future1)) {
            future = future1;
        } else {
            throw new UnsupportedOperationException(
                    "ContextAwareTaskRepository 暂不支持任务类型: " + spec.getClass().getName());
        }

        BackgroundTask task = new BackgroundTask(taskId, subAgentId, future);
        tasks.put(taskId, task);
        return task;
    }

    @Override
    public void removeTask(RuntimeContext rc, String sessionId, String taskId) {
        tasks.remove(taskId);
    }

    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public Collection<BackgroundTask> listTasks(
            RuntimeContext rc, String sessionId, TaskStatus filter) {
        if (filter == null) {
            return List.copyOf(tasks.values());
        }
        List<BackgroundTask> result = new ArrayList<>();
        for (BackgroundTask task : tasks.values()) {
            if (task.getTaskStatus() == filter) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    public boolean cancelTask(RuntimeContext rc, String sessionId, String taskId) {
        BackgroundTask task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        return task.cancel(true);
    }
}

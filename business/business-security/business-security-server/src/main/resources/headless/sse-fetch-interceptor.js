/**
 * SSE Fetch 拦截器
 *
 * 功能：拦截 window.fetch 请求，当检测到 /brain/run/copilotKit 的 SSE 响应时，
 * 克隆 ReadableStream，逐 chunk 解析 SSE 事件，通过 window.__onHeadlessSseEvent 回调。
 *
 * SSE 解析逻辑与 @ag-ui/client 的 parseSSEStream 一致：
 * 1. TextDecoder 解码 Uint8Array 为 UTF-8 字符串
 * 2. 按 \n\n 分割为独立事件块
 * 3. 每个块按 \n 分行，提取 data: 开头的行
 * 4. 多行 data 拼接后为一个完整 JSON
 */
(function () {
    // 防止重复注入
    if (window.__headlessFetchIntercepted) return;
    window.__headlessFetchIntercepted = true;

    const originalFetch = window.fetch;
    const SSE_URL_PATTERN = '/brain/run/copilotKit';
    const decoder = new TextDecoder('utf-8', { fatal: false });

    window.fetch = function (...args) {
        const request = originalFetch.apply(this, args);

        return request.then(response => {
            const url = typeof args[0] === 'string' ? args[0] : args[0]?.url || '';
            const contentType = response.headers.get('content-type') || '';

            if (url.includes(SSE_URL_PATTERN) && contentType.includes('text/event-stream')) {
                // 克隆 ReadableStream：stream1 给前端消费，stream2 给拦截器
                const [stream1, stream2] = response.body.tee();

                // 异步消费 stream2，解析 SSE 事件
                consumeSseStream(stream2);

                // 返回 stream1 给前端正常消费
                return new Response(stream1, {
                    status: response.status,
                    statusText: response.statusText,
                    headers: response.headers,
                });
            }

            return response;
        });
    };

    /**
     * 消费 SSE ReadableStream，逐 chunk 解析事件并回调
     */
    async function consumeSseStream(stream) {
        const reader = stream.getReader();
        let buffer = '';

        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                // 按 \n\n 分割为独立事件块
                const parts = buffer.split(/\n\n/);
                buffer = parts.pop() || '';

                for (const part of parts) {
                    const eventJson = parseSseBlock(part);
                    if (eventJson && window.__onHeadlessSseEvent) {
                        window.__onHeadlessSseEvent(eventJson);
                    }
                }
            }

            // 处理剩余 buffer
            if (buffer.trim()) {
                const eventJson = parseSseBlock(buffer);
                if (eventJson && window.__onHeadlessSseEvent) {
                    window.__onHeadlessSseEvent(eventJson);
                }
            }
        } catch (e) {
            console.error('[HeadlessSSE] Stream消费异常:', e);
        }
    }

    /**
     * 解析单个 SSE 事件块为 JSON 字符串
     * 格式：每行以 data: 开头，多行 data 拼接后 JSON.parse
     */
    function parseSseBlock(block) {
        const lines = block.split('\n');
        const dataLines = [];

        for (const line of lines) {
            if (line.startsWith('data:')) {
                dataLines.push(line.slice(5).replace(/^ /, ''));
            }
        }

        if (dataLines.length === 0) return null;

        const joined = dataLines.join('\n');
        try {
            // 验证是否为有效 JSON
            JSON.parse(joined);
            return joined;
        } catch {
            return null;
        }
    }
})();

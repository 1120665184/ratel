package org.quyq.gwsu.headless.core.session;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeadlessBrowserSessionTest {

    @Test
    void shouldWaitForAgentRunResponseWhenDispatchingMessage() {
        Page page = mock(Page.class);
        Response response = mock(Response.class);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return response;
        }).when(page).waitForResponse(
                org.mockito.ArgumentMatchers.<Predicate<Response>>any(),
                any(Page.WaitForResponseOptions.class),
                any(Runnable.class));

        Map<String, Object> payload = Map.of("text", "第二次问题");
        HeadlessBrowserSession.dispatchHeadlessMessage(page, payload);

        verify(page).waitForResponse(
                org.mockito.ArgumentMatchers.<Predicate<Response>>any(),
                any(Page.WaitForResponseOptions.class),
                any(Runnable.class));
        verify(page).evaluate(anyString(), any());
    }

    @Test
    void shouldBlockScreenshotUntilUserAnswerClickCompletes() throws Exception {
        BrowserContext context = mock(BrowserContext.class);
        Page page = mock(Page.class);
        Locator submitLocator = mock(Locator.class);
        when(context.newPage()).thenReturn(page);
        when(page.locator("[data-testid='headless-question-submit']")).thenReturn(submitLocator);
        when(page.isClosed()).thenReturn(false);
        when(page.screenshot(any(Page.ScreenshotOptions.class))).thenReturn(new byte[]{1});

        HeadlessBrowserSession session = new HeadlessBrowserSession(context, 0, mock(CacheUtils.class));
        HeadlessPageWrapper wrapper = pageWrapperOf(session);
        CompletableFuture<Object> screenshotFuture = new CompletableFuture<>();

        doAnswer(invocation -> {
            CompletableFuture.runAsync(() -> {
                try {
                    screenshotFuture.complete(wrapper.screenshot());
                } catch (Throwable error) {
                    screenshotFuture.completeExceptionally(error);
                }
            });
            Thread.sleep(100);
            assertFalse(screenshotFuture.isDone(), "点击提交期间不应允许并发截图操作同一个 Page");
            return null;
        }).when(submitLocator).click();

        session.submitUserAnswer("tool-1", Map.of("问题", "回答"), null);

        screenshotFuture.get(1, TimeUnit.SECONDS);
    }

    private HeadlessPageWrapper pageWrapperOf(HeadlessBrowserSession session) throws ReflectiveOperationException {
        Field field = HeadlessBrowserSession.class.getDeclaredField("pageWrapper");
        field.setAccessible(true);
        return (HeadlessPageWrapper) field.get(session);
    }
}

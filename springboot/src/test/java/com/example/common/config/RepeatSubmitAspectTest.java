package com.example.common.config;

import com.example.common.annotation.NoRepeatSubmit;
import com.example.entity.Apply;
import com.example.entity.Notice;
import com.example.exception.CustomException;
import com.example.support.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 防重复提交：同一份内容连点会被拦，内容不同的连续操作不拦，失败后可以立即重试。
 */
class RepeatSubmitAspectTest {

    private final RepeatSubmitAspect aspect = new RepeatSubmitAspect();

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @NoRepeatSubmit
    void annotated() {
    }

    private static NoRepeatSubmit annotation() throws NoSuchMethodException {
        return RepeatSubmitAspectTest.class.getDeclaredMethod("annotated").getAnnotation(NoRepeatSubmit.class);
    }

    private static ProceedingJoinPoint call(Object arg, Object result) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("ApplyController.updateById(..)");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{arg});
        when(joinPoint.proceed()).thenReturn(result);
        return joinPoint;
    }

    private static Apply review(int id, String status) {
        Apply apply = new Apply();
        apply.setId(id);
        apply.setStatus(status);
        return apply;
    }

    @Test
    @DisplayName("同一份内容连点两次：第二次被拦")
    void sameContentTwiceIsRejected() throws Throwable {
        CurrentUser.as("ADMIN", 9101, "管理员");

        assertEquals("ok", aspect.around(call(review(12, "审核通过"), "ok"), annotation()));
        CustomException e = assertThrows(CustomException.class,
                () -> aspect.around(call(review(12, "审核通过"), "ok"), annotation()));
        assertEquals("4090", e.getCode());
    }

    @Test
    @DisplayName("连着审批两条不同的请假：都放行")
    void differentContentIsAllowed() throws Throwable {
        CurrentUser.as("ADMIN", 9102, "管理员");

        assertEquals("ok", aspect.around(call(review(12, "审核通过"), "ok"), annotation()));
        assertEquals("ok", aspect.around(call(review(13, "审核通过"), "ok"), annotation()));
    }

    @Test
    @DisplayName("业务异常后可以立即重试")
    void failureAllowsImmediateRetry() throws Throwable {
        CurrentUser.as("ADMIN", 9103, "管理员");
        ProceedingJoinPoint failing = call(review(14, "审核通过"), null);
        when(failing.proceed()).thenThrow(new CustomException("5027", "已审核"));

        assertThrows(CustomException.class, () -> aspect.around(failing, annotation()));
        assertEquals("ok", aspect.around(call(review(14, "审核通过"), "ok"), annotation()));
    }

    @Test
    @DisplayName("指纹按内容算：没写 equals/hashCode 的实体，内容相同指纹也相同")
    void fingerprintFollowsContent() {
        Notice a = new Notice();
        a.setTitle("期末考试安排");
        Notice b = new Notice();
        b.setTitle("期末考试安排");
        Notice c = new Notice();
        c.setTitle("停课通知");

        assertEquals(RepeatSubmitAspect.fingerprint(new Object[]{a}), RepeatSubmitAspect.fingerprint(new Object[]{b}));
        assertNotEquals(RepeatSubmitAspect.fingerprint(new Object[]{a}), RepeatSubmitAspect.fingerprint(new Object[]{c}));
    }
}

package com.mealguide.mealguide_api.global.config.swagger;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

public final class SwaggerAnnotationSupport {

    private SwaggerAnnotationSupport() {}

    public static SwaggerApiResponses findSwaggerApiResponses(HandlerMethod handlerMethod) {
        Method implMethod = handlerMethod.getMethod();

        // 1) 구현 메서드에 직접 붙은 경우
        SwaggerApiResponses ann = AnnotatedElementUtils.findMergedAnnotation(implMethod, SwaggerApiResponses.class);
        if (ann != null) return ann;

        // 2) 구현 클래스가 implements 한 인터페이스 메서드에 붙은 경우
        Class<?> beanType = handlerMethod.getBeanType();
        for (Class<?> itf : beanType.getInterfaces()) {
            try {
                Method itfMethod = itf.getMethod(implMethod.getName(), implMethod.getParameterTypes());
                ann = AnnotatedElementUtils.findMergedAnnotation(itfMethod, SwaggerApiResponses.class);
                if (ann != null) return ann;
            } catch (NoSuchMethodException ignored) {
            }
        }

        // 3) (선택) 상위 클래스 메서드에 붙은 경우까지
        Class<?> superCls = beanType.getSuperclass();
        while (superCls != null && superCls != Object.class) {
            try {
                Method superMethod = superCls.getDeclaredMethod(implMethod.getName(), implMethod.getParameterTypes());
                ann = AnnotatedElementUtils.findMergedAnnotation(superMethod, SwaggerApiResponses.class);
                if (ann != null) return ann;
            } catch (NoSuchMethodException ignored) {
            }
            superCls = superCls.getSuperclass();
        }

        return null;
    }
}

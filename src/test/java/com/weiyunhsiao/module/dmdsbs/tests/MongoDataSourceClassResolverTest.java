package com.weiyunhsiao.module.dmdsbs.tests;

import com.weiyunhsiao.module.dmdsbs.annotation.MongoDS;
import com.weiyunhsiao.module.dmdsbs.resolver.MongoDataSourceClassResolver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Objects;

import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS1;

@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("MongoDataSourceClassResolverTest")
public class MongoDataSourceClassResolverTest {

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在Interface上時應該可以被正確偵測到")
    public void Should_GetValue_When_AnnotationOnInterface() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        MockInterfaceWithAnnotation target = new MockInterfaceWithAnnotationImpl();

        String dsName = resolver.findKey(
                MockInterfaceWithAnnotation.class.getDeclaredMethod("mockMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在實際方法上時應該可以被正確偵測到")
    public void Should_GetValue_When_AnnotationOnTargetMethod() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        MockInterface target = new MockInterfaceImpl();

        //傳入的Method是target method 能解析到
        String dsName = resolver.findKey(
                MockInterfaceImpl.class.getDeclaredMethod("mockMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);

        //傳入的Method並不是target method 也能解析到
        dsName = resolver.findKey(
                MockInterface.class.getDeclaredMethod("mockMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在實際方法上, 但傳入了橋接方法時應該可以被正確偵測到")
    public void Should_GetValue_When_AnnotationOnBridgeMethod() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        MockInterface target = new MockInterfaceImpl();

        //實際上橋接方法也會繼承原方法上的Annotation, 因@MongoDS有加上@Retention(RetentionPolicy.RUNTIME)
        //所以這個測試並沒有太多意義
        Method bridgeMethod = null;
        for (Method method : MockInterfaceImpl.class.getMethods()) {
            log.info("{}: Bridge: {}, Return type: {}, Annotation: {}", method.getName(), method.isBridge(), method.getReturnType(), method.getAnnotation(MongoDS.class));

            if (method.getName().equals("testBridge") && method.isBridge()) {
                bridgeMethod = method;
            }
        }

        Assertions.assertTrue(Objects.nonNull(bridgeMethod));

        String dsName = resolver.findKey(
                bridgeMethod,
                target
        );

        Assertions.assertEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在Class上時應該可以被正確偵測到")
    public void Should_GetValue_When_AnnotationOnClass() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        MockInterfaceImpl2 target = new MockInterfaceImpl2();

        String dsName = resolver.findKey(
                MockInterfaceImpl2.class.getMethod("mockNoAnnotationMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在Super Interface上時不會被正確偵測到")
    public void ShouldNot_GetValue_When_AnnotationOnSuperInterface() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        CombineMockInterfaceImpl target = new CombineMockInterfaceImpl();

        String dsName = resolver.findKey(
                CombineMockInterfaceImpl.class.getMethod("mockMethod", String.class),
                target
        );

        Assertions.assertNotEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當Annotation加在Super Class上時應該可以被正確偵測到")
    public void Should_GetValue_When_AnnotationOnSuperClass() {
        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        MockInterfaceImpl3 target = new MockInterfaceImpl3();

        //注意: 這邊是getMethod, 也就是說方法並不是由MockInterfaceImpl3宣告的, 這種情況實作會直接找MockInterfaceImpl2.mockNoAnnotationMethod 、 MockInterfaceImpl2
        String dsName = resolver.findKey(
                MockInterfaceImpl3.class.getMethod("mockNoAnnotationMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);

        //注意: 這邊是getDeclaredMethod, 會從子Class向上查詢, 上下兩種情況結果相同, 但路不同
        dsName = resolver.findKey(
                MockInterfaceImpl3.class.getDeclaredMethod("newMockNoAnnotationMethod", String.class),
                target
        );

        Assertions.assertEquals(DS1, dsName);
    }

    @SneakyThrows
    @Test
    @DisplayName("當使用cglib作為proxy時也能正確偵測到")
    public void Should_GetValue_When_CGLIBProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(MockInterfaceWithAnnotationImpl.class);
        enhancer.setCallback(new TestingInterceptor());
        MockInterfaceWithAnnotationImpl proxy = (MockInterfaceWithAnnotationImpl) enhancer.create();

        MongoDataSourceClassResolver resolver = new MongoDataSourceClassResolver(true);
        String dsName = resolver.findKey(
                MockInterfaceWithAnnotationImpl.class.getDeclaredMethod("mockMethod", String.class),
                proxy
        );
        Assertions.assertEquals(DS1, dsName);
    }


    @MongoDS(DS1)
    public interface MockInterfaceWithAnnotation {
        void mockMethod(String data);
    }

    public static class MockInterfaceWithAnnotationImpl implements MockInterfaceWithAnnotation {
        @Override
        public void mockMethod(String data) {
        }
    }

    public interface MockInterface<T extends CharSequence> {
        void mockMethod(String data);

        T testBridge();
    }

    public static class MockInterfaceImpl implements MockInterface<StringBuilder> {

        @Override
        @MongoDS(DS1)
        public void mockMethod(String data) {
        }

        @Override
        @MongoDS(DS1)
        public StringBuilder testBridge() {
            return new StringBuilder("ABC");
        }
    }

    @MongoDS(DS1)
    public static class MockInterfaceImpl2 extends MockInterfaceImpl {

        public void mockNoAnnotationMethod(String data) {
        }
    }

    public interface CombineMockInterface extends MockInterfaceWithAnnotation {
    }

    public static class CombineMockInterfaceImpl implements CombineMockInterface {

        @Override
        public void mockMethod(String data) {
        }
    }

    public static class MockInterfaceImpl3 extends MockInterfaceImpl2 {
        public void newMockNoAnnotationMethod(String data) {
        }
    }

    private class TestingInterceptor implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            Object result = proxy.invokeSuper(obj, args);
            return result;
        }
    }
}

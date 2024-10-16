package com.weiyunhsiao.module.dmdsbs.tests;

import com.weiyunhsiao.module.dmdsbs.annotation.MongoDS;
import com.weiyunhsiao.module.dmdsbs.aop.DynamicMongoDataSourceAnnotationAdvisor;
import com.weiyunhsiao.module.dmdsbs.aop.DynamicMongoDataSourceAnnotationInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;

import java.util.Objects;

import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS1;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("TestPointcutAdvisor")
public class TestPointcutAdvisor {

    //lenient=true (寬容模式, 對stub的檢查更加寬鬆)
    @Mock(lenient = true)
    private DynamicMongoDataSourceAnnotationInterceptor interceptor;

    private DynamicMongoDataSourceAnnotationAdvisor advisor;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        advisor = new DynamicMongoDataSourceAnnotationAdvisor(interceptor, MongoDS.class);
    }

    @SneakyThrows
    public void tearDown(){
        if(Objects.nonNull(closeable)){
            closeable.close();
        }
    }

    @Test
    @DisplayName("當方法上有@MongoDS時AOP應該要生效")
    public void Should_Advise_When_AnnotationOnTargetMethod() throws Throwable {

        ProxyFactory factory = new ProxyFactory(new TargetInterfaceThatNoAnnotationImpl());
        factory.addAdvisor(advisor);
        TargetInterfaceThatNoAnnotation proxy = (TargetInterfaceThatNoAnnotation) factory.getProxy();

        when(interceptor.invoke(any(MethodInvocation.class))).thenReturn("");

        proxy.testWithAnnotation();

        verify(interceptor).invoke(any(MethodInvocation.class));
    }

    @Test
    @DisplayName("當方法上沒有@DS時AOP應該要不生效")
    public void ShouldNot_Advise_When_AnnotationOnTargetMethod() throws Throwable {

        ProxyFactory factory = new ProxyFactory(new TargetInterfaceThatNoAnnotationImpl());
        factory.addAdvisor(advisor);
        TargetInterfaceThatNoAnnotation proxy = (TargetInterfaceThatNoAnnotation) factory.getProxy();

        when(interceptor.invoke(any(MethodInvocation.class))).thenReturn("");

        proxy.test();

        verify(interceptor, never()).invoke(any(MethodInvocation.class));
    }

    @Test
    @DisplayName("當interface上有@DS時AOP應該要生效")
    public void Should_Advise_When_AnnotationOnInterface() throws Throwable {

        ProxyFactory factory = new ProxyFactory(new TargetInterfaceThatWithAnnotationImpl());
        factory.addAdvisor(advisor);
        TargetInterfaceThatWithAnnotation proxy = (TargetInterfaceThatWithAnnotation) factory.getProxy();

        when(interceptor.invoke(any(MethodInvocation.class))).thenReturn("");

        proxy.test();

        verify(interceptor).invoke(any(MethodInvocation.class));
    }

    @Test
    @DisplayName("當class上有@DS時AOP應該要生效")
    public void Should_Advise_When_AnnotationOnClass() throws Throwable {

        ProxyFactory factory = new ProxyFactory(new TargetInterfaceThatAnnotationOnClass());
        factory.addAdvisor(advisor);
        TargetInterfaceThatNoAnnotation proxy = (TargetInterfaceThatNoAnnotation) factory.getProxy();

        when(interceptor.invoke(any(MethodInvocation.class))).thenReturn("");

        proxy.test();

        verify(interceptor).invoke(any(MethodInvocation.class));
    }

    @Test
    @DisplayName("當被繼承的class上有@DS時AOP應該要生效")
    public void Should_Advise_When_AnnotationOnInheritedClass() throws Throwable {

        ProxyFactory factory = new ProxyFactory(new TargetInterfaceThatAnnotationOnInheritedClass());
        factory.addAdvisor(advisor);
        TargetInterfaceThatNoAnnotation proxy = (TargetInterfaceThatNoAnnotation) factory.getProxy();

        when(interceptor.invoke(any(MethodInvocation.class))).thenReturn("");

        proxy.test();

        verify(interceptor).invoke(any(MethodInvocation.class));
    }


    public interface TargetInterfaceThatNoAnnotation {
        String testWithAnnotation();

        String test();
    }

    public static class TargetInterfaceThatNoAnnotationImpl implements TargetInterfaceThatNoAnnotation {

        @Override
        @MongoDS(DS1)
        public String testWithAnnotation() {
            return "aaa";
        }

        @Override
        public String test() {
            return "";
        }
    }

    @MongoDS(DS1)
    public interface TargetInterfaceThatWithAnnotation {
        String test();
    }

    public static class TargetInterfaceThatWithAnnotationImpl implements TargetInterfaceThatWithAnnotation {

        @Override
        public String test() {
            return "";
        }
    }

    @MongoDS(DS1)
    public static class TargetInterfaceThatAnnotationOnClass implements TargetInterfaceThatNoAnnotation {

        @Override
        public String testWithAnnotation() {
            return "aaa";
        }

        @Override
        public String test() {
            return "";
        }
    }

    public static class TargetInterfaceThatAnnotationOnInheritedClass extends TargetInterfaceThatAnnotationOnClass {
        @Override
        public String test() {
            return "aaa";
        }
    }

}

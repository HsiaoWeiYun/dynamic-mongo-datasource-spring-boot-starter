package com.weiyunhsiao.module.dmdsbs.resolver;


import com.weiyunhsiao.module.dmdsbs.annotation.MongoDS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ClassUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class MongoDataSourceClassResolver {

    private final Map<Object, String> dsCache = new ConcurrentHashMap<>();
    private final boolean allowedPublicOnly;

    public MongoDataSourceClassResolver(boolean allowedPublicOnly) {
        this.allowedPublicOnly = allowedPublicOnly;
    }

    public String findKey(Method method, Object targetObject) {
        if (method.getDeclaringClass() == Object.class) {
            return "";
        }
        Object cacheKey = new MethodClassKey(method, targetObject.getClass());
        String ds = this.dsCache.get(cacheKey);
        if (ds == null) {
            ds = computeDatasource(method, targetObject);
            if (ds == null) {
                ds = "";
            }
            this.dsCache.put(cacheKey, ds);
        }
        return ds;
    }

    public void cleanCache() {
        dsCache.clear();
    }

    /**
     * 查詢順序: 當前方法->原始方法/橋接方法->原始物件->原始物件上每個interface->if(原始方法與實際執行的方法並不是同一個方法): {原始方法->原始方法的Class}
     * 查詢順序: ->if(目標物件非代理): 每一層繼承物件
     */
    private String computeDatasource(Method method, Object targetObject) {
        //檢查是否限制為public才需要檢查
        if (allowedPublicOnly && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }


        //從當前方法(當前方法!=原始方法, 有可能是interface上的abstract method)上直接找Annotation
        String dsAttr = findDataSourceAttribute(method);
        if (dsAttr != null) {
            return dsAttr;
        }

        //取得目標Class
        Class<?> targetClass = targetObject.getClass();

        //嘗試獲取原始Class (如果 target 是代理的話) (ClassUtils.getUserClass 只處理CGLIB代理)
        Class<?> userClass = ClassUtils.getUserClass(targetClass);

        //獲取目標類上的method (傳入的method有可能是interface method)
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);

        //如果原始方法是一個橋接方法, 則返回橋接方法, 但如果不是則回傳原始方法
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

        //針對可能是橋接方法的方法查找Annotation
        dsAttr = findDataSourceAttribute(specificMethod);
        if (dsAttr != null) {
            return dsAttr;
        }

        //從原始物件上查找Annotation
        dsAttr = findDataSourceAttribute(userClass);
        if (dsAttr != null && ClassUtils.isUserLevelMethod(method)) {
            return dsAttr;
        }


        //從原始物件這邊實作的interface上查找Annotation, 先找到的為主
        for (Class<?> interfaceClazz : ClassUtils.getAllInterfacesForClassAsSet(userClass)) {
            dsAttr = findDataSourceAttribute(interfaceClazz);
            if (dsAttr != null) {
                return dsAttr;
            }
        }

        //如果為true, 代表原始方法與實際執行的方法並不是同一個方法, 可能是橋接方法亦或者傳入的是父類宣告的方法 (getDeclaredMethod、getMethod)
        if (specificMethod != method) {

            dsAttr = findDataSourceAttribute(method);
            if (dsAttr != null) {
                return dsAttr;
            }

            //從原始方法宣告的物件上開始查找Annotation
            dsAttr = findDataSourceAttribute(method.getDeclaringClass());
            if (dsAttr != null && ClassUtils.isUserLevelMethod(method)) {
                return dsAttr;
            }
        }
        return getDefaultDataSourceAttr(targetObject);
    }


    private String getDefaultDataSourceAttr(Object targetObject) {
        Class<?> targetClass = targetObject.getClass();

        //不是代理的話就開始一層一層的去查找繼承關的的物件上有沒有Annotation, 從子向上找
        if (!Proxy.isProxyClass(targetClass)) {
            Class<?> currentClass = targetClass;
            while (currentClass != Object.class) {
                String datasourceAttr = findDataSourceAttribute(currentClass);
                if (datasourceAttr != null) {
                    return datasourceAttr;
                }
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }


    private String findDataSourceAttribute(AnnotatedElement ae) {
        AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ae, MongoDS.class);
        if (attributes != null) {
            return attributes.getString("value");
        }
        return null;
    }
}

/*
 *
 *  (C) Copyright 2016 Ymatou (http://www.ymatou.com/).
 *  All rights reserved.
 *
 */

package com.ymatou.doorgod.apigateway.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author luoshiqian 2016/10/12 10:14
 */
public class BeanUtils {

    /** logger. */
    private static Logger logger = LoggerFactory.getLogger(BeanUtils.class);

    /** getter prefix length. */
    public static final int LENGTH_GETTER_PREFIX = "get".length();

    /** 保护的构造方法. */
    protected BeanUtils() {
    }

    /**
     * 循环向上转型,获取对象的DeclaredField.
     *
     * @param object
     *            对象实例
     * @param propertyName
     *            属性名
     * @return 返回对应的Field
     * @throws NoSuchFieldException
     *             如果没有该Field时抛出
     */
    public static Field getDeclaredField(Object object, String propertyName)
            throws NoSuchFieldException {
        Assert.notNull(object);
        Assert.hasText(propertyName);

        return getDeclaredField(object.getClass(), propertyName);
    }

    /**
     * 循环向上转型,获取对象的DeclaredField.
     *
     * @param clazz
     *            类型
     * @param propertyName
     *            属性名
     * @return 返回对应的Field
     * @throws NoSuchFieldException
     *             如果没有该Field时抛出.
     */
    public static Field getDeclaredField(Class clazz, String propertyName)
            throws NoSuchFieldException {
        Assert.notNull(clazz);
        Assert.hasText(propertyName);

        for (Class superClass = clazz; superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            try {
                return superClass.getDeclaredField(propertyName);
            } catch (NoSuchFieldException ex) {
                // Field不在当前类定义,继续向上转型
                logger.debug(ex.getMessage(), ex);
            }
        }

        throw new NoSuchFieldException("No such field: " + clazz.getName()
                + '.' + propertyName);
    }

    /**
     * 暴力获取对象变量值,忽略private,protected修饰符的限制.
     *
     * @param object
     *            对象实例
     * @param propertyName
     *            属性名
     * @return 强制获得属性值
     * @throws NoSuchFieldException
     *             如果没有该Field时抛出.
     */
    public static Object forceGetProperty(Object object, String propertyName)
            throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(object, propertyName, true);
    }

    public static Object safeGetFieldValue(Object object, String fieldName) {
        return safeGetFieldValue(object, fieldName, true);
    }

    public static Object safeGetFieldValue(Object object, String fieldName,
                                           boolean targetAccessible) {
        try {
            return getFieldValue(object, fieldName, targetAccessible);
        } catch (NoSuchFieldException ex) {
            logger.warn("", ex);
        } catch (IllegalAccessException ex) {
            logger.warn("", ex);
        }

        return null;
    }

    public static Object getFieldValue(Object object, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        return getFieldValue(object, fieldName, false);
    }

    public static Object getFieldValue(Object object, String fieldName,
                                       boolean targetAccessible) throws NoSuchFieldException,
            IllegalAccessException {
        Assert.notNull(object);
        Assert.hasText(fieldName);

        Field field = getDeclaredField(object, fieldName);

        boolean accessible = field.isAccessible();
        field.setAccessible(targetAccessible);

        Object result = field.get(object);

        field.setAccessible(accessible);

        return result;
    }

    /**
     * 暴力设置对象变量值,忽略private,protected修饰符的限制.
     *
     * @param object
     *            对象实例
     * @param propertyName
     *            属性名
     * @param newValue
     *            赋予的属性值
     * @throws NoSuchFieldException
     *             如果没有该Field时抛出.
     */
    public static void forceSetProperty(Object object, String propertyName,
                                        Object newValue) throws NoSuchFieldException,
            IllegalAccessException {
        setFieldValue(object, propertyName, newValue, true);
    }

    public static void safeSetFieldValue(Object object, String fieldName,
                                         Object newValue) {
        safeSetFieldValue(object, fieldName, newValue, true);
    }

    public static void safeSetFieldValue(Object object, String fieldName,
                                         Object newValue, boolean targetAccessible) {
        try {
            setFieldValue(object, fieldName, newValue, targetAccessible);
        } catch (NoSuchFieldException ex) {
            logger.warn("", ex);
        } catch (IllegalAccessException ex) {
            logger.warn("", ex);
        }
    }

    public static void setFieldValue(Object object, String propertyName,
                                     Object newValue, boolean targetAccessible)
            throws NoSuchFieldException, IllegalAccessException {
        Assert.notNull(object);
        Assert.hasText(propertyName);

        Field field = getDeclaredField(object, propertyName);

        boolean accessible = field.isAccessible();
        field.setAccessible(targetAccessible);

        field.set(object, newValue);

        field.setAccessible(accessible);
    }

    /**
     * 暴力调用对象函数,忽略private,protected修饰符的限制.
     *
     * @param object
     *            对象实例
     * @param methodName
     *            方法名
     * @param params
     *            方法参数
     * @return Object 方法调用返回的结果对象
     * @throws NoSuchMethodException
     *             如果没有该Method时抛出.
     */
    public static Object invokePrivateMethod(Object object, String methodName,
                                             Object... params) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        return invokeMethod(object, methodName, true, params);
    }

    public static Object safeInvokeMethod(Object object, Method method,
                                          Object... params) {
        try {
            return method.invoke(object, params);
        } catch (IllegalAccessException ex) {
            logger.warn("", ex);
        } catch (InvocationTargetException ex) {
            logger.warn("", ex);
        }

        return null;
    }

    public static Object safeInvokeMethod(Object object, String methodName,
                                          Object... params) {
        try {
            return invokeMethod(object, methodName, params);
        } catch (NoSuchMethodException ex) {
            logger.warn("", ex);
        } catch (IllegalAccessException ex) {
            logger.warn("", ex);
        } catch (InvocationTargetException ex) {
            logger.warn("", ex);
        }

        return null;
    }

    public static Object invokeMethod(Object object, String methodName,
                                      Object... params) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        return invokeMethod(object, methodName, false, params);
    }

    public static Object invokeMethod(Object object, String methodName,
                                      boolean targetAccessible, Object... params)
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Assert.notNull(object);
        Assert.hasText(methodName);

        Class[] types = new Class[params.length];

        for (int i = 0; i < params.length; i++) {
            types[i] = params[i].getClass();
        }

        Class clazz = object.getClass();
        Method method = null;

        for (Class superClass = clazz; superClass != Object.class; superClass = superClass
                .getSuperclass()) {
            try {
                method = superClass.getDeclaredMethod(methodName, types);

                break;
            } catch (NoSuchMethodException ex) {
                // 方法不在当前类定义,继续向上转型
                logger.debug(ex.getMessage(), ex);
            }
        }

        if (method == null) {
            throw new NoSuchMethodException("No Such Method : "
                    + clazz.getSimpleName() + "." + methodName
                    + Arrays.asList(types));
        }

        boolean accessible = method.isAccessible();
        method.setAccessible(targetAccessible);

        Object result = method.invoke(object, params);

        method.setAccessible(accessible);

        return result;
    }

    /**
     * 按Field的类型取得Field列表.
     *
     * @param object
     *            对象实例
     * @param type
     *            类型
     * @return 属性对象列表
     */
    public static List<Field> getFieldsByType(Object object, Class type) {
        List<Field> list = new ArrayList<Field>();
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().isAssignableFrom(type)) {
                list.add(field);
            }
        }

        return list;
    }

    /**
     * 按FieldName获得Field的类型.
     *
     * @param type
     *            类型
     * @param name
     *            属性名
     * @return 属性的类型
     * @throws NoSuchFieldException
     *             指定属性不存在时，抛出异常
     */
    public static Class getPropertyType(Class type, String name)
            throws NoSuchFieldException {
        return getDeclaredField(type, name).getType();
    }


    public static String getFieldName(String methodName) {
        String fieldName = methodName.substring(LENGTH_GETTER_PREFIX);

        return fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
    }
}

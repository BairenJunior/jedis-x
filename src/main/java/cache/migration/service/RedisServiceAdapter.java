/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package cache.migration.service;

import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cache.service.MemcachedService;
import cache.service.RedisService;

/**
 * Redis服务适配器，适配{@link MemcachedService}接口，用于迁移Memcached到Redis。
 * <p>
 * 【XML配置示例】
 * 
 * <pre>
 * {@literal 
 * <bean id="redisServiceAdapter" class="cache.migration.service.RedisServiceAdapter" />
 * }
 * </pre>
 * 
 * @author huagang.li 2014年12月17日 上午11:36:35
 */
public class RedisServiceAdapter implements MemcachedService {

    private static final RedisFuture<Boolean> FUTURE_TRUE = new RedisFuture<Boolean>(Boolean.TRUE);

    /** Redis服务 */
    @Autowired
    private RedisService                      redisService;

    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // 适配器不用开关功能
    }

    @Override
    public void close() {
        redisService.close();
    }

    /**
     * 不支持该操作，因为通过Eclipse的调用层次结构(Call Hierarchy)功能已核实
     * {@link cn.fraudmetrix.forseti.biz.service.intf.MemcachedService#set(String, int, Object)}方法并未在项目中使用。
     */
    @Override
    public Future<Boolean> set(String key, int exp, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Boolean> set(String key, int exp, String value) {
        if (exp <= 0) {
            redisService.set(key, value);
        } else {
            redisService.setex(key, exp, value);
        }
        return FUTURE_TRUE;
    }

    @Override
    public Object get(String key) {
        return redisService.get(key);
    }

    @Override
    public String getString(String key) {
        return redisService.get(key);
    }

    @Override
    public Future<Object> getAsync(String key) {
        if (StringUtils.isNotBlank(key)) {
            String value = redisService.get(key);
            if (null != value) {
                return new RedisFuture<Object>(value);
            }
        }
        return null;
    }

    @Override
    public Future<Boolean> append(String key, int exp, String value) {
        if (StringUtils.isNotBlank(key) && null != value) {
            String origin = this.getString(key);
            String newValue = (origin == null) ? value : (origin + value);
            if (exp <= 0) {
                redisService.set(key, newValue);
            } else {
                redisService.setex(key, exp, newValue);
            }
        }
        return FUTURE_TRUE;
    }

    @Override
    public Future<Boolean> delete(String key) {
        if (StringUtils.isNotBlank(key)) {
            long removedKeyNum = redisService.del(key);
            if (removedKeyNum > 0) {
                return FUTURE_TRUE;
            }
        }
        return null;
    }

}

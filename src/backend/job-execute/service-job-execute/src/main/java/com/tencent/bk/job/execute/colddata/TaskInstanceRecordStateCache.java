/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.colddata;

import com.tencent.bk.job.common.redis.BaseRedisCache;
import com.tencent.bk.job.execute.model.TaskInstanceRecordStateDO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 作业实例记录状态缓存
 */
@Slf4j
@Component
public class TaskInstanceRecordStateCache extends BaseRedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1小时过期
    private static final int EXPIRE_HOURS = 1;

    @Autowired
    public TaskInstanceRecordStateCache(@Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
                                        MeterRegistry meterRegistry) {
        super(meterRegistry, "TaskInstanceRecordStateCache");
        this.redisTemplate = redisTemplate;
    }

    /**
     * 更新
     *
     * @param recordStateDO 更新数据
     */
    public void addOrUpdate(TaskInstanceRecordStateDO recordStateDO) {
        String key = buildKey(recordStateDO.getTaskInstanceId());
        redisTemplate.opsForValue().set(key, recordStateDO, EXPIRE_HOURS, TimeUnit.HOURS);
    }

    public TaskInstanceRecordStateDO get(Long taskInstanceId) {
        String key = buildKey(taskInstanceId);
        Object cacheObj = redisTemplate.opsForValue().get(key);
        return cacheObj != null ? (TaskInstanceRecordStateDO) cacheObj : null;
    }

    private String buildKey(Long taskInstanceId) {
        return "job:taskInstance:state:" + taskInstanceId;
    }
}

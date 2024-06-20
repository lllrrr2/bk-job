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

package com.tencent.bk.job.common.service.quota;

import com.tencent.bk.job.common.refreshable.config.ConfigRefreshHandler;
import com.tencent.bk.job.common.service.quota.config.ResourceQuotaLimitProperties;
import com.tencent.bk.job.common.service.quota.config.parser.ResourceQuotaConfigParser;
import com.tencent.bk.job.common.service.quota.config.parser.RunningJobQuotaConfigParser;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 配额限制存储
 */
@Slf4j
public class ResourceQuotaStore implements ConfigRefreshHandler {

    /**
     * key: QuotaResourceId; value: ResourceQuotaLimit
     */
    private volatile Map<String, ResourceQuotaLimit> resourceQuotas = new HashMap<>();
    /**
     * 是否初始化
     */
    private volatile boolean isInitial = false;


    public ResourceQuotaLimit getResourceQuota(String resourceId) {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    load(true);
                }
            }
        }
        return resourceQuotas.get(resourceId);
    }

    public boolean load(boolean ignoreException) {
        boolean loadResult = true;
        try {
            loadInternal();
        } catch (Throwable e) {
            log.warn("Load ResourceQuota error", e);
            loadResult = false;
            if (ignoreException) {
                log.warn("Ignore ResourceQuota load error");
            } else {
                throw e;
            }
        }
        return loadResult;
    }

    private void loadInternal() {
        synchronized (this) {
            log.info("Load ResourceQuota start ...");
            ResourceQuotaLimitProperties resourceQuotaLimitProperties =
                ApplicationContextRegister.getBean(ResourceQuotaLimitProperties.class);

            if (resourceQuotaLimitProperties.getResources() == null
                || resourceQuotaLimitProperties.getResources().isEmpty()) {
                log.info("ResourceQuota empty!");
                return;
            }

            log.info("Parse ResourceQuota config: {}", JsonUtils.toJson(resourceQuotaLimitProperties));

            Map<String, ResourceQuotaLimit> tmpResourceQuotaLimits = new HashMap<>();
            resourceQuotaLimitProperties.getResources()
                .forEach((resourceId, resourceQuotaConfig) -> {
                    ResourceQuotaLimit resourceQuotaLimit = parseResourceQuotaConfig(resourceId, resourceQuotaConfig);
                    tmpResourceQuotaLimits.put(resourceId, resourceQuotaLimit);
                });

            // 使用新的配置完全替换老的配置
            resourceQuotas = tmpResourceQuotaLimits;
            log.info("Load ResourceQuota config done! resourceQuotas: {}", resourceQuotas);
            isInitial = true;
        }
    }

    /**
     * 解析特性配置
     *
     * @param resourceId             资源 ID
     * @param resourceQuotaLimitProp 配置信息
     * @throws ResourceQuotaConfigParseException 如果解析报错，抛出异常
     */
    private ResourceQuotaLimit parseResourceQuotaConfig(
        String resourceId,
        ResourceQuotaLimitProperties.ResourceQuotaLimitProp resourceQuotaLimitProp
    ) throws ResourceQuotaConfigParseException {

        if (StringUtils.isBlank(resourceId)) {
            log.error("resourceId is blank");
            throw new ResourceQuotaConfigParseException("resourceId is blank");
        }

        ResourceQuotaConfigParser configParser = chooseResourceQuotaConfigParser(resourceId);
        return configParser.parse(resourceQuotaLimitProp);
    }


    private ResourceQuotaConfigParser chooseResourceQuotaConfigParser(String resourceId) {
        ResourceQuotaConfigParser configParser;
        switch (resourceId) {
            case QuotaResourceId.JOB_INSTANCE:
                configParser = new RunningJobQuotaConfigParser();
                break;
            default:
                log.error("Unsupported resource: {}", resourceId);
                throw new ResourceQuotaConfigParseException("Unsupported resource: " + resourceId);
        }
        return configParser;
    }

    @Override
    public boolean handleConfigChange() {
        return load(true);
    }
}

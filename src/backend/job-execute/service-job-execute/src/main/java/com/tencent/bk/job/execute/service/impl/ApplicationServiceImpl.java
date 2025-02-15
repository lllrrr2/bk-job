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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.cc.config.CcConfig;
import com.tencent.bk.job.common.cc.sdk.EsbCcClient;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.ApplicationResourceClient;
import com.tencent.bk.job.execute.client.SyncResourceClient;
import com.tencent.bk.job.execute.model.db.CacheAppDO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@DependsOn({"ccConfigSetter"})
@Service
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationResourceClient applicationResourceClient;
    private final SyncResourceClient syncResourceClient;
    private final RedisTemplate redisTemplate;
    private final EsbCcClient ccClient;

    @Autowired
    public ApplicationServiceImpl(ApplicationResourceClient applicationResourceClient,
                                  @Qualifier("jsonRedisTemplate") RedisTemplate redisTemplate,
                                  EsbConfig esbConfig,
                                  CcConfig ccConfig,
                                  QueryAgentStatusClient queryAgentStatusClient,
                                  SyncResourceClient syncResourceClient,
                                  MeterRegistry meterRegistry) {
        this.applicationResourceClient = applicationResourceClient;
        this.redisTemplate = redisTemplate;
        this.ccClient = new EsbCcClient(esbConfig, ccConfig, queryAgentStatusClient, meterRegistry);
        this.syncResourceClient = syncResourceClient;
    }

    @Override
    public ApplicationInfoDTO getAppById(long appId) {
        ServiceApplicationDTO returnApp = applicationResourceClient.queryAppById(appId);
        if (returnApp == null) {
            ApplicationInfoDTO appInfo = ccClient.getAppById(appId, null, null);
            log.info("Query app from cmdb, appId:{}, app:{}", appId, appInfo);
            return appInfo;
        } else {
            return convertToApplicationInfoDTO(returnApp);
        }
    }

    private ApplicationInfoDTO convertToApplicationInfoDTO(ServiceApplicationDTO app) {
        ApplicationInfoDTO applicationInfo = new ApplicationInfoDTO();
        applicationInfo.setId(app.getId());
        applicationInfo.setName(app.getName());
        applicationInfo.setAppType(AppTypeEnum.valueOf(app.getAppType()));
        applicationInfo.setSubAppIds(app.getSubAppIds());
        applicationInfo.setMaintainers(app.getMaintainers());
        applicationInfo.setOperateDeptId(app.getOperateDeptId());
        applicationInfo.setLanguage(app.getLanguage());
        return applicationInfo;
    }

    @Override
    public List<ApplicationInfoDTO> listAllApps() {
        List<ServiceApplicationDTO> apps = syncResourceClient.listAllApps();
        if (apps == null) {
            return Collections.emptyList();
        }
        return apps.stream().map(this::convertToApplicationInfoDTO).collect(Collectors.toList());
    }

    @Override
    public List<Long> listAllAppIds() {
        List<ServiceApplicationDTO> apps = syncResourceClient.listAllApps();
        if (apps == null) {
            return Collections.emptyList();
        }
        return apps.stream().map(ServiceApplicationDTO::getId).collect(Collectors.toList());
    }

    @Override
    public CacheAppDO getAppPreferCache(long appId) {
        Object appObj = redisTemplate.opsForHash().get("job:execute:apps", String.valueOf(appId));
        if (appObj == null) {
            log.info("App is not in cache, get from job-manage module!");
            ApplicationInfoDTO appInfo = getAppById(appId);
            if (appInfo == null) {
                return null;
            }
            CacheAppDO cacheAppDO = CacheAppDO.fromApplicationInfoDTO(appInfo);
            try {
                log.info("Refresh app cache, app:{}", JsonUtils.toJson(cacheAppDO));
                redisTemplate.opsForHash().put("job:execute:apps", String.valueOf(appId), cacheAppDO);
            } catch (Exception e) {
                log.warn("Refresh app cache fail", e);
            }
            return cacheAppDO;
        }
        return (CacheAppDO) appObj;
    }
}

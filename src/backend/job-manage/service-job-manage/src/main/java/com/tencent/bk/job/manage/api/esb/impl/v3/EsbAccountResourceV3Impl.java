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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.v3.EsbAccountV3Resource;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetAccountListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbAccountV3DTO;
import com.tencent.bk.job.manage.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbAccountResourceV3Impl implements EsbAccountV3Resource {
    private final AccountService accountService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbAccountResourceV3Impl(AccountService accountService,
                                    AppScopeMappingService appScopeMappingService) {
        this.accountService = accountService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_account_list"})
    public EsbResp<EsbPageDataV3<EsbAccountV3DTO>> getAccountListUsingPost(EsbGetAccountListV3Req request) {
        request.fillAppResourceScope(appScopeMappingService);
        long appId = request.getAppId();
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        int start = 0;
        if (request.getStart() != null && request.getStart() > 0) {
            start = request.getStart();
        }
        baseSearchCondition.setStart(start);
        int length = 20;
        if (request.getLength() != null && request.getLength() > 0) {
            length = request.getLength();
        }
        baseSearchCondition.setLength(length);
        List<AccountDTO> accountList = accountService.listAllAppAccount(appId, null, baseSearchCondition);
        List<EsbAccountV3DTO> accountV3DTOList = convertToEsbAccountV3DTOList(accountList);
        Integer accountCount = accountService.countAllAppAccount(appId, null);
        EsbPageDataV3<EsbAccountV3DTO> esbPageData = new EsbPageDataV3<>();
        esbPageData.setTotal(accountCount.longValue());
        esbPageData.setStart(start);
        esbPageData.setLength(length);
        esbPageData.setData(accountV3DTOList);
        return EsbResp.buildSuccessResp(esbPageData);
    }

    private List<EsbAccountV3DTO> convertToEsbAccountV3DTOList(List<AccountDTO> accounts) {
        List<EsbAccountV3DTO> esbAccounts = new ArrayList<>();
        if (accounts == null || accounts.isEmpty()) {
            return esbAccounts;
        }
        for (AccountDTO account : accounts) {
            EsbAccountV3DTO esbAccount = account.toEsbAccountV3DTO();
            esbAccounts.add(esbAccount);
        }
        return esbAccounts;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbAccountV3DTO>> getAccountList(String username,
                                                                  String appCode,
                                                                  Long bkBizId,
                                                                  String scopeType,
                                                                  String scopeId,
                                                                  Integer category,
                                                                  Integer start,
                                                                  Integer length) {
        EsbGetAccountListV3Req request = new EsbGetAccountListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBkBizId(bkBizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setCategory(category);
        request.setStart(start);
        request.setLength(length);
        return getAccountListUsingPost(request);
    }
}

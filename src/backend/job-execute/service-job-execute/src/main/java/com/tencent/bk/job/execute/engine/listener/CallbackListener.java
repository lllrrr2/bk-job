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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.model.JobCallbackDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * 任务执行结束回调处理
 */
@Component
@Slf4j
public class CallbackListener extends BaseJobMqListener {

    /**
     * 处理回调请求
     */
    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        JobCallbackDTO callbackDTO = (JobCallbackDTO) message.getPayload();
        long taskInstanceId = callbackDTO.getId();
        try {
            log.info("Handle callback, taskInstanceId: {}, msg: {}", taskInstanceId, callbackDTO);
            String callbackUrl = callbackDTO.getCallbackUrl();
            try {
                new URL(callbackUrl);
            } catch (MalformedURLException var5) {
                log.warn("Callback fail, bad url: {}", callbackUrl);
                return;
            }
            callbackDTO.setCallbackUrl(null);
            try {
                // TODO 需要优化，返回application/json
                try {
                    String rst = HttpConPoolUtil.post(callbackUrl, JsonUtils.toJson(callbackDTO));
                    log.info("Callback success, taskInstanceId: {}, result: {}", taskInstanceId, rst);
                } catch (Throwable e) { //出错重试一次
                    String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
                    log.warn(errorMsg, e);
                    String rst = HttpConPoolUtil.post(callbackUrl, JsonUtils.toJson(callbackDTO));
                    log.info("Retry callback success, taskInstanceId: {}, result: {}", taskInstanceId, rst);
                }
            } catch (Throwable e) {
                String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
                log.warn(errorMsg, e);
            }
        } catch (Throwable e) {
            String errorMsg = "Callback fail, taskInstanceId: " + taskInstanceId;
            log.warn(errorMsg, e);
        }
    }
}

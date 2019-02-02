/*
 * Copyright 2015 Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nls.client.example;

import com.alibaba.nls.client.AccessToken;

/**
 * CreateTokenDemo class
 *
 * 根据AccessKeyId和AccessKeySecret生成token
 * @author siwei
 * @date 2018/5/29
 */
public class CreateTokenDemo {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("CreateTokenDemo need params: <AccessKeyId> <AccessKeySecret>");
            System.exit(-1);
        }
        String akId = args[0];
        String akSecrete = args[1];
        try {
            AccessToken accessToken = AccessToken.apply(akId, akSecrete);
            System.out.println("Created token: " + accessToken.getToken() +
                    // 有效时间，单位为秒
                    ", expire time(s): " + accessToken.getExpireTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

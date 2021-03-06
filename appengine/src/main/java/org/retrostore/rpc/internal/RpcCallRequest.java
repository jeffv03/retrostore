/*
 * Copyright 2017, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.retrostore.rpc.internal;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.retrostore.data.app.AppManagement;
import org.retrostore.data.user.UserManagement;
import org.retrostore.data.user.UserService;
import org.retrostore.request.Request;
import org.retrostore.request.RequestData;
import org.retrostore.request.Responder;
import org.retrostore.resources.ImageServiceWrapper;
import org.retrostore.rpc.AddEditAppRpcCall;
import org.retrostore.rpc.AddEditUserRpcCall;
import org.retrostore.rpc.AdminUserListRpcCall;
import org.retrostore.rpc.AppListRpcCall;
import org.retrostore.rpc.DeleteAppRpcCall;
import org.retrostore.rpc.DeleteDiskImageRpcCall;
import org.retrostore.rpc.DeleteScreenshotRpcCall;
import org.retrostore.rpc.DeleteUserRpcCall;
import org.retrostore.rpc.GetAppFormDataRpcCall;
import org.retrostore.rpc.GetSiteContextRpcCall;
import org.retrostore.rpc.ListDiskImagesRpcCall;
import org.retrostore.rpc.ListScreenshotsRpcCall;
import org.retrostore.rpc.PublicAppListRpcCall;
import org.retrostore.rpc.ReorderScreenshotsRpcCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Serves RPC calls
 */
public class RpcCallRequest implements Request {
  private static final Logger LOG = Logger.getLogger("RpcCallRequest");

  private static final String RPC_PREFIX = "/rpc";
  private static final String RPC_METHOD_PARAM = "m";

  private final Map<String, RpcCall<RpcParameters>> mRpcCalls;


  public RpcCallRequest(UserManagement userManagement,
                        AppManagement appManagement,
                        ImageServiceWrapper imageService) {
    // Note: Add new RPC calls here.
    List<RpcCall<RpcParameters>> calls = ImmutableList.of(
        new AdminUserListRpcCall(userManagement),
        new GetSiteContextRpcCall(userManagement),
        new AddEditUserRpcCall(userManagement),
        new AddEditAppRpcCall(appManagement, userManagement),
        new GetAppFormDataRpcCall(appManagement),
        new DeleteUserRpcCall(userManagement),
        new AppListRpcCall(appManagement),
        new PublicAppListRpcCall(appManagement, imageService),
        new DeleteAppRpcCall(appManagement),
        new ListDiskImagesRpcCall(appManagement),
        new DeleteDiskImageRpcCall(appManagement),
        new ListScreenshotsRpcCall(appManagement),
        new DeleteScreenshotRpcCall(appManagement),
        new ReorderScreenshotsRpcCall(appManagement));

    Map<String, RpcCall<RpcParameters>> callsMapped = new HashMap<>();
    for (RpcCall<RpcParameters> call : calls) {
      if (callsMapped.containsKey(call.getName())) {
        LOG.severe("RPC call name conflict: " + call.getName());
      }
      callsMapped.put(call.getName(), call);
    }
    mRpcCalls = ImmutableMap.copyOf(callsMapped);
  }

  @Override
  public boolean serveUrl(RequestData requestData, Responder responder,
                          UserService accountTypeProvider) {
    String url = requestData.getUrl();
    if (!url.startsWith(RPC_PREFIX)) {
      return false;
    }

    Optional<String> method = requestData.getString(RPC_METHOD_PARAM);
    if (!method.isPresent()) {
      responder.respondBadRequest("No method name specified.");
    } else if (!mRpcCalls.containsKey(method.get())) {
      responder.respondBadRequest(String.format("RPC method '%s' not found.", method));
    } else {
      RpcCall<RpcParameters> rpcCall = mRpcCalls.get(method.get());
      if (!rpcCall.isPermitted(accountTypeProvider.getForCurrentUser())) {
        responder.respondBadRequest("Current user not permitted.");
      } else {
        RpcParameters params = new RpcParametersImpl(requestData);
        rpcCall.call(params, responder);
      }
    }
    return true;
  }
}

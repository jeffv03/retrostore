/*
 * Copyright 2017, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.retrostore.rpc.api;

import com.google.gson.Gson;
import org.retrostore.client.common.ListAppsApiParams;
import org.retrostore.client.common.proto.ApiResponseApps;
import org.retrostore.client.common.proto.App;
import org.retrostore.data.app.AppManagement;
import org.retrostore.data.app.AppStoreItem;
import org.retrostore.request.RequestData;
import org.retrostore.request.Responder;
import org.retrostore.request.Response;
import org.retrostore.resources.ImageServiceWrapper;
import org.retrostore.rpc.internal.ApiCall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * API call to list apps from the store.
 */
public class ListAppsApiCall implements ApiCall {
  private static final Logger LOG = Logger.getLogger("ListAppsApiCall");
  private static final int SCREENSHOT_SIZE = 800;
  private final AppManagement mAppManagement;
  private final ApiHelper mApiHelper;

  public ListAppsApiCall(AppManagement appManagement, ImageServiceWrapper imageService) {
    mAppManagement = appManagement;
    mApiHelper = new ApiHelper(appManagement, imageService);
  }

  @Override
  public String getName() {
    return "listApps";
  }

  @Override
  public Response call(final RequestData data) {
    final ApiResponseApps responseApps = callInternal(data);
    return new Response() {
      @Override
      public void respond(Responder responder) {
        responder.respondProto(responseApps);
      }
    };
  }

  private ApiResponseApps callInternal(RequestData data) {
    long tStart = System.currentTimeMillis();
    ApiResponseApps.Builder response = ApiResponseApps.newBuilder();
    ListAppsApiParams params = parseParams(data.getBody());
    if (params == null) {
      return response.setSuccess(false).setMessage("Cannot parse parameters.").build();
    }

    // TODO: This is not efficient once we have a large number of apps.
    List<AppStoreItem> allApps = mAppManagement.getAllApps();
    if (allApps.size() - 1 < params.start) {
      return response.setSuccess(false).setMessage("Parameter 'start' out of range").build();
    }

    long tPreBuilding = System.currentTimeMillis();
    LOG.fine("[Perf] getAllApps: " + (tStart - tPreBuilding));
    List<App.Builder> apps = new ArrayList<>();
    for (int i = params.start; i < params.start + params.num && i < allApps.size(); ++i) {
      apps.add(mApiHelper.convert(allApps.get(i)));
    }
    LOG.fine("[Perf] Building list: " + (tPreBuilding - System.currentTimeMillis()));

    // Sort the output alphabetically.
    Collections.sort(apps, new Comparator<App.Builder>() {
      @Override
      public int compare(App.Builder o1, App.Builder o2) {
        if (o1 == null || o2 == null) {
          return 0;
        }
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (App.Builder app : apps) {
      response.addApp(app.build());
    }
    return response.setSuccess(true).setMessage("All good :-)").build();
  }

  private ListAppsApiParams parseParams(String params) {
    try {
      return (new Gson()).fromJson(params, ListAppsApiParams.class);
    } catch (Exception ex) {
      LOG.log(Level.WARNING, "Cannot parse params", ex);
      return null;
    }
  }
}

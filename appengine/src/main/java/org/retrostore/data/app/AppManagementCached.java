/*
 *  Copyright 2017, Sascha Häberling
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.retrostore.data.app;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A caching layer for app management, with the same interface.
 */
public class AppManagementCached implements AppManagement {
  /** A real app management implementation. */
  private final AppManagement mAppManagement;

  /** TODO: Handle sorting. */
  /** Important: These items are not immutable and a client might change them. */
  private final Map<String, AppStoreItem> mAppCacheById;
  private final Map<Long, MediaImage> mMediaCacheById;
  private final Map<Long, Author> mAuthorCacheById;

  public AppManagementCached(AppManagement appManagement) {
    mAppManagement = Preconditions.checkNotNull(appManagement);
    mAppCacheById = new HashMap<>();
    mMediaCacheById = new HashMap<>();
    mAuthorCacheById = new HashMap<>();

    // Important: Update cache at the beginning so we can then keep it updates throughout with
    // incremental updates only.
    // Note once we have a lot of apps this is no longer pracitcal. It might not fit in memory and
    // will be very expensive to perform.
    updateAppCache();
  }

  @Override
  public void addOrChangeApp(AppStoreItem app) {
    mAppManagement.addOrChangeApp(app);
    mAppCacheById.put(app.id, app);
  }

  @Override
  public Optional<AppStoreItem> getAppById(String id) {
    if (mAppCacheById.containsKey(id)) {
      return Optional.of(mAppCacheById.get(id));
    }

    Optional<AppStoreItem> appOpt = mAppManagement.getAppById(id);
    if (appOpt.isPresent()) {
      mAppCacheById.put(appOpt.get().id, appOpt.get());
    }
    return appOpt;
  }

  @Override
  public boolean addScreenshot(String appId, String blobKey) {
    boolean success = mAppManagement.addScreenshot(appId, blobKey);
    updateAppCacheItem(appId);
    return success;
  }

  @Override
  public boolean removeScreenshot(String appId, String blobKey) {
    boolean success = mAppManagement.removeScreenshot(appId, blobKey);
    updateAppCacheItem(appId);
    return success;
  }

  @Override
  public long addMediaImage(String appId, String filename, byte[] data) {
    return mAppManagement.addMediaImage(appId, filename, data);
  }

  @Override
  public Map<Long, MediaImage> getMediaImagesForApp(String appId) {
    // TODO: We should think about how to cache these better. Probably need to key by appId.
    return mAppManagement.getMediaImagesForApp(appId);
  }

  @Override
  public void deleteMediaImage(long mediaId) {
    mMediaCacheById.remove(mediaId);
    mAppManagement.deleteMediaImage(mediaId);
  }

  @Override
  public long[] deleteMediaImagesForApp(String appId) {
    long[] deleted = mAppManagement.deleteMediaImagesForApp(appId);
    for (long mediaId : deleted) {
      mMediaCacheById.remove(mediaId);
    }
    return deleted;
  }

  @Override
  public List<AppStoreItem> getAllApps() {
    return new ArrayList<>(mAppCacheById.values());
  }

  @Override
  public void removeApp(String id) {
    mAppManagement.removeApp(id);
    mAppCacheById.remove(id);
  }

  @Override
  public long ensureAuthorExists(String name) {
    long id = mAppManagement.ensureAuthorExists(name);
    updateAuthorCache();
    return id;
  }

  @Override
  public List<Author> listAuthors() {
    if (mAuthorCacheById.isEmpty()) {
      updateAuthorCache();
    }
    return new ArrayList<>(mAuthorCacheById.values());
  }

  @Override
  public Optional<Author> getAuthorById(long id) {
    if (mAuthorCacheById.isEmpty()) {
      updateAuthorCache();
    }
    return Optional.fromNullable(mAuthorCacheById.get(id));
  }

  private void updateAppCacheItem(String id) {
    Optional<AppStoreItem> appOpt = mAppManagement.getAppById(id);
    if (!appOpt.isPresent()) {
      return;
    }
    AppStoreItem app = appOpt.get();
    mAppCacheById.put(app.id, app);
  }

  private void updateAppCache() {
    List<AppStoreItem> apps = mAppManagement.getAllApps();
    mAppCacheById.clear();
    for (AppStoreItem app : apps) {
      mAppCacheById.put(app.id, app);
    }
  }

  private void updateAuthorCache() {
    List<Author> authors = mAppManagement.listAuthors();
    mAuthorCacheById.clear();
    for (Author author : authors) {
      mAuthorCacheById.put(author.id, author);
    }
  }
}

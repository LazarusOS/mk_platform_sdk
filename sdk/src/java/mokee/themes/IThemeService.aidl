/*
 * Copyright (C) 2014-2016 The CyanogenMod Project
 * Copyright (C) 2014-2016 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mokee.themes;

import mokee.themes.IThemeChangeListener;
import mokee.themes.IThemeProcessingListener;
import mokee.themes.ThemeChangeRequest;

import java.util.Map;

/** {@hide} */
interface IThemeService {
    oneway void requestThemeChangeUpdates(in IThemeChangeListener listener);
    oneway void removeUpdates(in IThemeChangeListener listener);

    oneway void requestThemeChange(in ThemeChangeRequest request, boolean removePerAppThemes);
    oneway void applyDefaultTheme();
    boolean isThemeApplying();
    int getProgress();

    boolean processThemeResources(String themePkgName);
    boolean isThemeBeingProcessed(String themePkgName);
    oneway void registerThemeProcessingListener(in IThemeProcessingListener listener);
    oneway void unregisterThemeProcessingListener(in IThemeProcessingListener listener);

    oneway void rebuildResourceCache();

    long getLastThemeChangeTime();
    int getLastThemeChangeRequestType();
}

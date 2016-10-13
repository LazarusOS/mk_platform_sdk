/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2016 The MoKee Open Source Project
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
package org.mokee.internal.mkparts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import mokee.platform.Manifest;

import static com.android.internal.R.styleable.Preference;
import static com.android.internal.R.styleable.Preference_fragment;
import static com.android.internal.R.styleable.Preference_icon;
import static com.android.internal.R.styleable.Preference_key;
import static com.android.internal.R.styleable.Preference_summary;
import static com.android.internal.R.styleable.Preference_title;
import static mokee.platform.R.styleable.mk_Searchable;
import static mokee.platform.R.styleable.mk_Searchable_xmlRes;

public class PartsList {

    private static final String TAG = PartsList.class.getSimpleName();

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String ACTION_PART_CHANGED = "org.mokee.mkparts.PART_CHANGED";
    public static final String ACTION_REFRESH_PART = "org.mokee.mkparts.REFRESH_PART";

    public static final String EXTRA_PART = "part";
    public static final String EXTRA_PART_KEY = "key";
    public static final String EXTRA_PART_SUMMARY = "summary";

    public static final String MKPARTS_PACKAGE = "org.mokee.mkparts";

    public static final ComponentName MKPARTS_ACTIVITY = new ComponentName(
            MKPARTS_PACKAGE, MKPARTS_PACKAGE + ".PartsActivity");

    public static  final ComponentName MKPARTS_REFRESHER = new ComponentName(
            MKPARTS_PACKAGE, MKPARTS_PACKAGE + ".RefreshReceiver");

    public static final String PARTS_ACTION_PREFIX = MKPARTS_PACKAGE + ".parts";

    private final Map<String, PartInfo> mParts = new ArrayMap<>();

    private final Map<String, Set<PartInfo.RemotePart>> mRemotes = new ArrayMap<>();

    private final Context mContext;

    private static PartsList sInstance;
    private static final Object sInstanceLock = new Object();

    private PartsList(Context context) {
        mContext = context;
        loadParts();
    }

    public static PartsList get(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new PartsList(context);
            }
            return sInstance;
        }
    }

    private void loadParts() {
        synchronized (mParts) {
            final PackageManager pm = mContext.getPackageManager();
            try {
                final Resources r = pm.getResourcesForApplication(MKPARTS_PACKAGE);
                if (r == null) {
                    return;
                }
                int resId = r.getIdentifier("parts_catalog", "xml", MKPARTS_PACKAGE);
                if (resId > 0) {
                    loadPartsFromResourceLocked(r, resId, mParts);
                }
            } catch (PackageManager.NameNotFoundException e) {
                // no mkparts installed
            }
        }
    }

    public Set<String> getPartsList() {
        synchronized (mParts) {
            return mParts.keySet();
        }
    }

    public PartInfo getPartInfo(String key) {
        synchronized (mParts) {
            return mParts.get(key);
        }
    }

    public final PartInfo getPartInfoForClass(String clazz) {
        synchronized (mParts) {
            for (PartInfo info : mParts.values()) {
                if (info.getFragmentClass() != null && info.getFragmentClass().equals(clazz)) {
                    return info;
                }
            }
            return null;
        }
    }

    private void loadPartsFromResourceLocked(Resources res, int resid,
                                             Map<String, PartInfo> target) {
        XmlResourceParser parser = null;

        try {
            parser = res.getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
                // Parse next until start tag is found
            }

            String nodeName = parser.getName();
            if (!"parts-catalog".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <parts-catalog> tag; found "
                                + nodeName + " at " + parser.getPositionDescription());
            }

            final int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("part".equals(nodeName)) {
                    TypedArray sa = res.obtainAttributes(attrs, Preference);

                    String key = null;
                    TypedValue tv = sa.peekValue(Preference_key);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            key = res.getString(tv.resourceId);
                        } else {
                            key = String.valueOf(tv.string);
                        }
                    }
                    if (key == null) {
                        throw new RuntimeException("Attribute 'key' is required");
                    }

                    final PartInfo info = new PartInfo(key);

                    tv = sa.peekValue(Preference_title);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setTitle(res.getString(tv.resourceId));
                        } else {
                            info.setTitle(String.valueOf(tv.string));
                        }
                    }

                    tv = sa.peekValue(Preference_summary);
                    if (tv != null && tv.type == TypedValue.TYPE_STRING) {
                        if (tv.resourceId != 0) {
                            info.setSummary(res.getString(tv.resourceId));
                        } else {
                            info.setSummary(String.valueOf(tv.string));
                        }
                    }

                    info.setFragmentClass(sa.getString(Preference_fragment));
                    info.setIconRes(sa.getResourceId(Preference_icon, 0));

                    sa = res.obtainAttributes(attrs, mk_Searchable);
                    info.setXmlRes(sa.getResourceId(mk_Searchable_xmlRes, 0));

                    sa.recycle();

                    target.put(key, info);

                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing catalog", e);
        } finally {
            if (parser != null) parser.close();
        }
    }

    public void registerRemotePart(final String key, final PartInfo.RemotePart remote) {
        synchronized (mParts) {
            if (DEBUG) {
                Log.v(TAG, "registerRemotePart part=" + key + " remote=" + remote.toString());
            }
            if (mRemotes.size() == 0) {
                final IntentFilter filter = new IntentFilter(ACTION_PART_CHANGED);
                mContext.registerReceiver(mPartChangedReceiver, filter,
                        Manifest.permission.MANAGE_PARTS, null);
            }

            Set<PartInfo.RemotePart> remotes = mRemotes.get(key);
            if (remotes == null) {
                remotes = new ArraySet<PartInfo.RemotePart>();
                mRemotes.put(key, remotes);
            }
            remotes.add(remote);

            final Intent i = new Intent(ACTION_REFRESH_PART);
            i.setComponent(PartsList.MKPARTS_REFRESHER);

            i.putExtra(EXTRA_PART_KEY, key);

            // Send an ordered broadcast to request a refresh and receive the reply
            // on the BroadcastReceiver.
            mContext.sendOrderedBroadcastAsUser(i, UserHandle.CURRENT,
                    Manifest.permission.MANAGE_PARTS,
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            synchronized (mParts) {
                                refreshPartFromBundleLocked(getResultExtras(true));
                            }
                        }
                    }, null, Activity.RESULT_OK, null, null);
        }
    }


    private void refreshPartFromBundleLocked(Bundle result) {
        PartInfo info = mParts.get(result.getString(EXTRA_PART_KEY));
        if (info != null) {
            PartInfo updatedPart = (PartInfo) result.getParcelable(EXTRA_PART);
            if (updatedPart != null) {
                if (info.updateFrom(updatedPart)) {
                    Set<PartInfo.RemotePart> remotes = mRemotes.get(info.getName());
                    if (remotes != null && remotes.size() > 0) {
                        for (PartInfo.RemotePart remote : remotes) {
                            if (DEBUG) {
                                Log.d(TAG, "refresh remote=" + remote.toString() +
                                           " info=" + info.toString());
                            }
                            remote.onRefresh(mContext, info);
                        }
                    }
                }
            }
        }
    }

    public void unregisterRemotePart(String key, final PartInfo.RemotePart remote) {
        synchronized (mParts) {
            if (DEBUG) {
                Log.d(TAG, "unregisterRemotePart: " + key + " remote=" + remote.toString());
            }
            Set<PartInfo.RemotePart> remotes = mRemotes.get(key);
            if (remotes != null) {
                remotes.remove(remote);
                if (remotes.size() == 0) {
                    mRemotes.remove(key);
                    if (mRemotes.size() == 0) {
                        mContext.unregisterReceiver(mPartChangedReceiver);
                    }
                }
            }
        }
    }

    /**
     * Receiver for asynchronous updates
     */
    private final BroadcastReceiver mPartChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mParts) {
                if (DEBUG) {
                    Log.d(TAG, "PART_CHANGED: " + intent.toString() +
                            " bundle: " + intent.getExtras().toString());
                }
                refreshPartFromBundleLocked(intent.getExtras());
            }
        }
    };
}
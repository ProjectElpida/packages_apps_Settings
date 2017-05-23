/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge.anomaly.checker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WakeupAlarmAnomalyDetectorTest {
    private static final int ANOMALY_UID = 111;
    private static final int NORMAL_UID = 222;
    private static final long RUNNING_TIME_MS = 2 * DateUtils.HOUR_IN_MILLIS;
    private static final int ANOMALY_WAKEUP_COUNT = 500;
    private static final int NORMAL_WAKEUP_COUNT = 50;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatterySipper mAnomalySipper;
    @Mock
    private BatterySipper mNormalSipper;
    @Mock
    private BatteryStats.Uid mAnomalyUid;
    @Mock
    private BatteryStats.Uid mNormalUid;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private BatteryStats.Uid.Pkg mPkg;
    @Mock
    private BatteryStats.Counter mCounter;

    private WakeupAlarmAnomalyDetector mWakeupAlarmAnomalyDetector;
    private Context mContext;
    private List<BatterySipper> mUsageList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        doReturn(false).when(mBatteryUtils).shouldHideSipper(any());
        doReturn(RUNNING_TIME_MS).when(mBatteryUtils).calculateRunningTimeBasedOnStatsType(any(),
                anyInt());

        mAnomalySipper.uidObj = mAnomalyUid;
        doReturn(ANOMALY_UID).when(mAnomalyUid).getUid();
        mNormalSipper.uidObj = mNormalUid;
        doReturn(NORMAL_UID).when(mNormalUid).getUid();

        mUsageList = new ArrayList<>();
        mUsageList.add(mAnomalySipper);
        mUsageList.add(mNormalSipper);
        doReturn(mUsageList).when(mBatteryStatsHelper).getUsageList();

        mWakeupAlarmAnomalyDetector = spy(new WakeupAlarmAnomalyDetector(mContext));
        mWakeupAlarmAnomalyDetector.mBatteryUtils = mBatteryUtils;
    }

    @Test
    public void testDetectAnomalies_containsAnomaly_detectIt() {
        doReturn(ANOMALY_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mAnomalyUid);
        doReturn(NORMAL_WAKEUP_COUNT).when(mWakeupAlarmAnomalyDetector).getWakeupAlarmCountFromUid(
                mNormalUid);
        final Anomaly anomaly = new Anomaly.Builder()
                .setUid(ANOMALY_UID)
                .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                .build();

        List<Anomaly> mAnomalies = mWakeupAlarmAnomalyDetector.detectAnomalies(mBatteryStatsHelper);

        assertThat(mAnomalies).containsExactly(anomaly);
    }

    @Test
    public void testGetWakeupAlarmCountFromUid_countCorrect() {
        final ArrayMap<String, BatteryStats.Uid.Pkg> packageStats = new ArrayMap<>();
        final ArrayMap<String, BatteryStats.Counter> alarms = new ArrayMap<>();
        doReturn(alarms).when(mPkg).getWakeupAlarmStats();
        doReturn(NORMAL_WAKEUP_COUNT).when(mCounter).getCountLocked(anyInt());
        doReturn(packageStats).when(mAnomalyUid).getPackageStats();
        packageStats.put("", mPkg);
        alarms.put("1", mCounter);
        alarms.put("2", mCounter);

        assertThat(mWakeupAlarmAnomalyDetector.getWakeupAlarmCountFromUid(mAnomalyUid)).isEqualTo(
                2 * NORMAL_WAKEUP_COUNT);
    }
}
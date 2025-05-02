package com.galaxyjoy.hexviewer.sdkadbmob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.roy.admobwrapper.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import kotlin.random.Random

//version 20240427
object AdMobManager {

    private const val TAG = "roy93~AdMobManager"

    private lateinit var application: Application
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var isAppOpenShowing = false
    private var lastAppOpenLoadTime: Long = 0

    private const val APP_OPEN_AD_TIME_OUT = 4 * 60 * 60 * 1000L // 4 hours
    private const val SHOW_INTERSTITIAL_CHANCE = 40 // 40%
    const val TEST_VSMART_IRIS = "884670AFCACDD337E31BB6153C6DB17E"
    const val TEST_VIVO_Z9 = "05B522309BC31052952BBCD5CC85ACA8"

    private var currentDeviceGAID = ""
    private var isVIPMember = false
    private val listGAIDVipMember = ArrayList<String>()

    private var appPreferences: AppPreferences? = null
    private var currentActivity: WeakReference<Activity>? = null

    var interstitialListener: InterstitialAdListener? = null

    fun init(
        application: Application,
        onComplete: (Boolean, String) -> Unit,
    ) {
        appPreferences = AppPreferences.getInstance(application)
        appPreferences?.getGAIDList()?.let {
            listGAIDVipMember.addAll(it)
        }
        Log.d(TAG, "###init listGAIDVipMember $listGAIDVipMember")
        getGAID(application) { gaidCurrent ->
            this.application = application
            this.currentDeviceGAID = gaidCurrent
            isVIPMember = listGAIDVipMember.contains(gaidCurrent)
            Log.d(TAG, "###init Current device GAID: $gaidCurrent, isWhitelistedDevice: $isVIPMember")

            //set test devices for all Roy's devices
            setTestDeviceIds(
                TEST_VSMART_IRIS,
                TEST_VIVO_Z9,
            )

            //set vip member
            if (appPreferences?.isAddVIPMemberFirstInitSuccess() == true) {
                //do nothing
            } else {
//                var list = getMyListVipDevice()
//                addVIPMember(list)
//                appPreferences?.addVIPMemberFirstInitSuccess()
            }
            onComplete(true, gaidCurrent)
            CoroutineScope(Dispatchers.Default).launch {
                EventBus.sendEvent(true)
            }
        }
    }

    fun getMyListVipDevice(): ArrayList<String> {
        var list = ArrayList<String>()
        list.add("9ad0127d-04be-4b6c-937a-ca3ed7f650b9")//vsmart iris
        list.add("9b6499f2-d4de-4b9e-afdf-ac2a2b127fb1")//ss a50
        list.add("c09b2f04-e145-490c-96f9-dab620074104")//oppo f7
        list.add("c228aa08-bedd-4e6e-adf6-ae5e95bcddae")//vivo v15
        list.add("46259467-0ac4-49c4-a3a2-7d3db3ce4bda")//tecno spark 20 pro +
        list.add("1b7c3e3f-c709-4e85-b26f-dd74c4df2ed7")//vivo 1906
        list.add("adaa42e7-9cc6-4a8a-9c90-d4d87842b12c")//tecno spark go 2024
        list.add("f5a36a2f-5add-4315-a171-0f8dddab78c7")//ss s20u
        list.add("6fbb207d-341d-470d-bb0a-dddd79522b32")//ss a52
        list.add("40f8e222-cf7a-4fac-9913-6809c4c58817")//mipad 5
        list.add("932099db-d381-4b52-98dc-5b96ba8b4ff4")//oppo reno 2f
        list.add("a1339bd1-8ea5-47cd-969e-4b5721b576b7")//redmi note 8+
        list.add("3f2f21d2-85eb-451b-a1a5-003668ba6345")//zte blade
        list.add("261f772c-6a10-499c-b896-4157d9ab6a25")//ss a11
        list.add("460d3f5c-bbe2-46fc-841a-6381e3c93864")//redmi95
        list.add("49606ad7-5cee-43b4-9af7-8aa274644737")//redmi note 13 pro
        list.add("6cf051f8-83f5-43b7-8c1a-1d20ae1f8d93")//redmi pad pro
        list.add("da10cb05-5458-42df-ba86-630732356b35")//vivo z9
        list.add("8f6ccdc1-08fd-4611-abdf-f48bdadb5581")//tablet lenovo
        list.add("66e652de-79ef-4889-8074-9b482fd81b5a")//redmi a3
        list.add("4ed22dd8-e8fb-442e-a75e-081a3d977957")//ss s24u
        return list;
    }

    fun getGAID(context: Context, callback: (String) -> Unit) {
        Thread {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val id = info.id ?: ""
                callback(id)
            } catch (e: Exception) {
                callback("")
                Log.d("AdMobManager", "getGAID error $e")
            }
        }.start()
    }

    fun setCurrentActivity(activity: Activity) {
        this.currentActivity = WeakReference(activity)
    }

    //search logcat: "to get test ads on this device"
    fun setTestDeviceIds(vararg deviceIds: String) {
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(deviceIds.toList()).build()
        MobileAds.setRequestConfiguration(configuration)
        Log.d(TAG, "setTestDeviceIds deviceIds ${deviceIds.toList()}")
    }

    fun getTestDeviceIds(): List<String> {
        val testDeviceIds = MobileAds.getRequestConfiguration().testDeviceIds
        Log.d(TAG, "getTestDeviceIds testDeviceIds: $testDeviceIds")
        return testDeviceIds
    }

    fun loadBanner(
        context: Context,
        adUnitId: String,
        container: ViewGroup,
        adSize: AdSize = AdSize.LARGE_BANNER,
    ): AdView? {
        if (isVIPMember) {
            Log.d(TAG, "Banner Ad skipped due to whitelist device")
            return null
        }
        val adView = AdView(context).apply {
            setAdSize(adSize)
            setAdUnitId(adUnitId)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner Ad Loaded - Revenue +1 impression")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Banner Ad Failed to load: ${error.message}")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner Ad Clicked - Revenue +1 click")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner Ad Closed")
                }
            }
        }
        container.removeAllViews()
        container.addView(
            adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    fun loadInterstitial(
        context: Context,
        adUnitId: String,
    ) {
        if (isVIPMember) {
            Log.d(TAG, "Interstitial Ad skipped due to whitelist device")
            return
        }
        InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial Ad Loaded")
                interstitialAd = ad
                setInterstitialCallback()
                interstitialListener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "Interstitial Ad Failed to load: ${error.message}")
                interstitialAd = null
                interstitialListener?.onAdFailedToLoad(error)
            }
        })
    }

    private fun setInterstitialCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad Shown - Revenue +1 impression")
                interstitialListener?.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad Dismissed")
                interstitialAd = null
                currentActivity?.get()?.let {
                    loadInterstitial(it, BuildConfig.ADMOB_INTERSTITIAL_ID)
                }
                interstitialListener?.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                interstitialAd = null
                interstitialListener?.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                Log.d(TAG, "Interstitial Ad Clicked - Revenue +1 click")
                interstitialListener?.onAdClicked()
            }
        }
    }

    fun showInterstitial(activity: Activity) {
        if (isVIPMember) {
            Log.d(TAG, "Interstitial Show Skipped - Device in whitelist")
            interstitialListener?.onAdNotAvailable()
            return
        }

        val randomChance = Random.nextInt(1, 101)
        Log.d(TAG, "Random chance: $randomChance")
        if (randomChance > SHOW_INTERSTITIAL_CHANCE) {
            Log.d(TAG, "Skipped showing interstitial because random > $SHOW_INTERSTITIAL_CHANCE")
            interstitialListener?.onAdNotAvailable()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready")
            interstitialListener?.onAdNotAvailable()
        }
    }

    fun loadAppOpenAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: () -> Unit,
    ) {
        if (isVIPMember) {
            Log.d(TAG, "App Open Ad skipped due to whitelist device")
            return
        }
        if (isAppOpenLoading) {
            if (BuildConfig.DEBUG) {
                //do nothing
            } else {
                if ((System.currentTimeMillis() - lastAppOpenLoadTime) < APP_OPEN_AD_TIME_OUT) {
                    Log.d(TAG, "App Open Ad is still valid or loading")
                    return
                }
            }
        }
        isAppOpenLoading = true

        AppOpenAd.load(
            context, adUnitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad Loaded")
                    appOpenAd = ad
                    lastAppOpenLoadTime = System.currentTimeMillis()
                    isAppOpenLoading = false
                    onAdLoaded.invoke();
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "App Open Ad Failed to load: ${error.message}")
                    isAppOpenLoading = false
                }
            },
        )
    }

    fun showAppOpenAd(activity: Activity) {
        if (isVIPMember) {
            Log.d(TAG, "App Open Ad Show Skipped - Device in whitelist")
            return
        }
        if (isAppOpenShowing) {
            Log.d(TAG, "Already showing App Open Ad")
            return
        }
        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad Shown - Revenue +1 impression")
                    isAppOpenShowing = true
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App Open Ad Dismissed")
                    appOpenAd = null
                    isAppOpenShowing = false
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "App Open Ad Failed to Show: ${adError.message}")
                    appOpenAd = null
                    isAppOpenShowing = false
                }

                override fun onAdClicked() {
                    Log.d(TAG, "App Open Ad Clicked - Revenue +1 click")
                }
            }
            appOpenAd?.show(activity)
        } else {
            Log.d(TAG, "App Open Ad not ready")
        }
    }

    fun isVIPMember(): Boolean {
        return isVIPMember
    }

    fun addVIPMember(listGaidDevice: List<String>) {
        listGaidDevice.forEach { gaidDevice ->
            listGAIDVipMember.addIfNotExist(gaidDevice)
        }
        appPreferences?.saveGAIDList(listGAIDVipMember)
        isVIPMember = listGAIDVipMember.contains(currentDeviceGAID)
        Log.d(TAG, "setVIPMember listGaidDevice $listGaidDevice => isVIPMember $isVIPMember")
    }

    fun deleteVIPMember(listGaidDevice: List<String>) {
        listGaidDevice.forEach { gaidDevice ->
            listGAIDVipMember.remove(gaidDevice)
        }
        appPreferences?.saveGAIDList(listGAIDVipMember)
        isVIPMember = listGAIDVipMember.contains(currentDeviceGAID)
        Log.d(TAG, "deleteVIPMember listGaidDevice $listGaidDevice => isVIPMember $isVIPMember")
    }

    fun initSplashScreen(onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val parentJob = this // Tham chiếu đến coroutine cha
            // Khởi tạo Job xử lý timeout 2 giây
            val timeoutJob = launch {
                delay(2_000) // Đợi 2 giây
                onComplete.invoke()
                parentJob.cancel() // Hủy toàn bộ coroutine sau khi xử lý
            }
            // Bắt đầu collect giá trị từ Flow
            EventBus.eventFlow.collectLatest { value ->
                timeoutJob.cancel() // Hủy timeout khi nhận được giá trị
                Log.d("roy93~", "collectLatest: $value")
                if (value) {
                    onComplete.invoke()
                    parentJob.cancel() // Dừng collect sau khi xử lý giá trị true
                }
            }
        }
    }

    interface InterstitialAdListener {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: LoadAdError)
        fun onAdShowed()
        fun onAdDismissed()
        fun onAdClicked()
        fun onAdFailedToShow(error: AdError)
        fun onAdNotAvailable()
    }
}

fun <T> MutableList<T>.addIfNotExist(element: T) {
    if (!contains(element)) add(element)
}

class AppPreferences private constructor(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("loitp", Context.MODE_PRIVATE)

    private val keyListGAID = "keyListGAID"
    private val keyAddVIPMemberFirstInitSuccess = "keyAddVIPMemberFirstInitSuccess"

    fun saveGAIDList(list: List<String>) {
        sharedPref.edit {
            putStringSet(keyListGAID, list.toSet())
            // Chuyển List thành Set để tự động loại bỏ trùng lặp
        }
    }

    fun getGAIDList(): ArrayList<String> {
        val set = sharedPref.getStringSet(keyListGAID, emptySet()) ?: emptySet()
        return ArrayList(set) // Chuyển Set thành ArrayList
    }

    fun addVIPMemberFirstInitSuccess() {
        sharedPref.edit {
            putBoolean(keyAddVIPMemberFirstInitSuccess, true)
        }
    }

    fun isAddVIPMemberFirstInitSuccess(): Boolean {
        return sharedPref.getBoolean(keyAddVIPMemberFirstInitSuccess, false)
    }

    companion object {
        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

object EventBus {
    private val _eventFlow = MutableSharedFlow<Boolean>()
    val eventFlow = _eventFlow.asSharedFlow()

    suspend fun sendEvent(value: Boolean) {
        _eventFlow.emit(value)
    }
}

class AppLifecycleListener(
    private val callbackForegroundBackground: (
        isForeground: Boolean,
        activity: Activity,
    ) -> Unit,
    private val callbackActivityCreated: (
        activity: Activity,
    ) -> Unit,
) :
    Application.ActivityLifecycleCallbacks {

    private var activityCount = 0

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            // App moved to foreground
            callbackForegroundBackground(true, activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App moved to background
            callbackForegroundBackground(false, activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        callbackActivityCreated(activity)
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

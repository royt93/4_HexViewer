package com.galaxyjoy.hexviewer.feature.vip

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.galaxyjoy.hexviewer.BuildConfig
import com.galaxyjoy.hexviewer.MyApplication
import com.galaxyjoy.hexviewer.R
import com.galaxyjoy.hexviewer.databinding.FVipManagementBinding
import com.galaxyjoy.hexviewer.ui.util.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AppPreferences
import com.roy.sdkadbmob.UIUtils
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ActVipManagement : AppCompatActivity() {

    companion object {
        /** Số ngày VIP cấp cho mỗi lần "Xem quảng cáo" — khớp label `vip_watch_ad_3d` trong strings.xml. */
        private const val REWARD_VIP_DAYS = 3
    }

    private lateinit var binding: FVipManagementBinding
    private lateinit var vipPrefs: VipPrefs

    // Animators & Timers (Nullable for memory cleanup)
    private var countDownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var crownShimmerAnimator: ObjectAnimator? = null
    private var countUpAnimator: ValueAnimator? = null
    private var slideInAnimator: ValueAnimator? = null

    private var activateRunnable: Runnable? = null
    private var activateProgressDialog: androidx.appcompat.app.AlertDialog? = null

    private var lastMinute: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        binding = FVipManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.layoutVipRoot) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.contentContainer.setPadding(
                binding.contentContainer.paddingLeft,
                binding.contentContainer.paddingTop,
                binding.contentContainer.paddingRight,
                systemBars.bottom
            )
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        vipPrefs = VipPrefs(this)

        MyApplication.addLog(this, "ActVipManagement", "onCreate | windowSoftInputMode=${window.attributes.softInputMode}"
                + " | isVip=${AdManager.isVipByKeyActive()}")

        setupToolbar()
        setupClickListeners()
        setupInputListeners()
        bindUi()
        triggerSlideInAnimation()
    }

    override fun onResume() {
        super.onResume()
        MyApplication.addLog(this, "ActVipManagement", "onResume | isVip=${AdManager.isVipByKeyActive()}"
                + " | windowSoftInputMode=${window.attributes.softInputMode}")
        startLoopAnimations()
        bindUi() // Refresh state
        // Preload rewarded ad so it's ready when user taps "Watch ad → VIP"
        AdManager.requestPreloadRewarded()
    }

    override fun onPause() {
        MyApplication.addLog(this, "ActVipManagement", "onPause | etVipKey focused=${binding.etVipKey.isFocused}"
                + " | etVipKey text.len=${binding.etVipKey.text?.length ?: 0}")
        cancelLoopAnimations()
        super.onPause()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        // Activate via key
        binding.btnActivate.setOnClickListener {
            val inputKey = binding.etVipKey.text?.toString()?.trim() ?: ""
            MyApplication.addLog(this, "ActVipManagement", "btnActivate clicked | inputKey.len=${inputKey.length} | empty=${inputKey.isEmpty()}")
            if (inputKey.isEmpty()) {
                binding.tilVipKey.error = getString(R.string.vip_redeem_hint)
                return@setOnClickListener
            }
            binding.tilVipKey.error = null

            // Chặn double-tap: disable ngay lúc bấm — nếu không, tap thứ 2 trong lúc dialog cũ đang
            // hiện sẽ show thêm 1 dialog verifying nữa mà không dismiss dialog cũ (dialog cũ
            // setCancelable(false) nên kẹt màn hình vĩnh viễn tới khi Activity recreate/destroy).
            binding.btnActivate.isEnabled = false

            // Show verifying dialog — lưu ở field (không phải local val) để onDestroy dismiss được
            // nếu user thoát màn hình giữa lúc đang chờ (chống WindowLeaked, audit smoke2 #3).
            activateProgressDialog?.dismiss()
            activateProgressDialog = MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vip_verifying_title)
                .setView(R.layout.v_progress_dialog)
                .setCancelable(false)
                .create()
                .also { it.show() }

            // Simulating network or verification latency for better UX
            activateRunnable?.let { binding.root.removeCallbacks(it) }
            activateRunnable = Runnable {
                if (isFinishing) return@Runnable
                activateProgressDialog?.dismiss()
                activateProgressDialog = null
                binding.btnActivate.isEnabled = binding.etVipKey.text?.isNotEmpty() == true

                // SDK tự resolve: thử token ECDSA trước, rồi vipRedeemCodes (AdSdkConfig.vipRedeemCodes)
                // — app không tự lookup/validate nữa (audit F13/F18/F22: gọi cũ truyền lại
                // adConfig.vipKeySecret khiến verify của SDK thành no-op).
                val success = AdManager.activateVipByKey(this, inputKey, 0)
                MyApplication.addLog(this, "ActVipManagement", "activateVipByKey | success=$success")
                if (success) {
                    vipPrefs.markUserRedeemed()
                    showActivationSuccess()
                } else {
                    showActivationFailed()
                }
                activateRunnable = null
            }
            binding.root.postDelayed(activateRunnable!!, 1000)
        }

        // Revoke VIP
        binding.btnRevokeVip.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vip_revoke_all_confirm_title)
                .setMessage(R.string.vip_revoke_all_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    AdManager.clearVipByKey()
                    bindUi()
                    Toast.makeText(this, R.string.vip_revoked_message, Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Watch Ad to get 3-day VIP
        binding.btnWatchAd.setOnClickListener {
            // Offline: no ad can ever load — give immediate feedback instead of doing nothing
            if (!NetworkUtils.isNetworkAvailable(this)) {
                showNoAdDialog()
                return@setOnClickListener
            }
            AdManager.showRewarded(this) { earned ->
                if (isFinishing) return@showRewarded
                if (earned) {
                    grantVipFromAd()
                } else {
                    // Fallback Interstitial: CHỈ để bù đắp doanh thu khi rewarded không có sẵn.
                    // TUYỆT ĐỐI KHÔNG cấp VIP ở đây dù ad có shown hay không — cấp reward cho ad
                    // non-rewarded vi phạm thẳng chính sách Google/AppLovin Rewarded (Step 7 rule 6,
                    // audit F12: bug cũ cấp VIP qua nhánh này, rủi ro ban account).
                    AdManager.showInterstitial(this) { shown ->
                        if (isFinishing) return@showInterstitial
                        if (shown) showNoRewardDialog() else showNoAdDialog()
                    }
                }
            }
        }

        // Privacy Policy Footer
        binding.tvPrivacyPolicy.setOnClickListener {
            try {
                val url = BuildConfig.PRIVACY_POLICY_URL
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.vip_open_link_error, Toast.LENGTH_SHORT).show()
            }
        }

        // UMP/MAX privacy-options entry point: cho phép user mở lại lựa chọn consent sau lần đầu.
        binding.tvPrivacyChoices.setOnClickListener {
            AdManager.showConsentFormIfAvailable(this) {
                if (!isFinishing) bindUi()
            }
        }
    }

    private fun setupInputListeners() {
        // Disable activate button initially if key is empty
        val initialKey = binding.etVipKey.text?.toString()?.trim() ?: ""
        binding.btnActivate.isEnabled = initialKey.isNotEmpty()

        // Debug: track focus + keyboard visibility to diagnose keyboard-overlaps-edittext bug
        binding.etVipKey.setOnFocusChangeListener { v, hasFocus ->
            val rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(v)
            val imeVisible = rootInsets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) ?: false
            val imeHeight = rootInsets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())?.bottom ?: 0
            MyApplication.addLog(this, "ActVipManagement", "etVipKey focusChange | hasFocus=$hasFocus"
                    + " | imeVisible=$imeVisible | imeHeight=$imeHeight"
                    + " | windowSoftInputMode=${window.attributes.softInputMode}")
        }

        binding.etVipKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s?.toString()?.trim() ?: ""
                binding.btnActivate.isEnabled = input.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun grantVipFromAd() {
        // grantVipDays = nguồn TIN CẬY nội bộ (reward đã earned), KHÔNG qua verify key/token, chạy cả
        // release (audit F13/F22: đường cũ gọi activateVipByKey(vipKeySecret) fail im lặng vì nhánh
        // legacy plaintext mặc định TẮT). KHÔNG markUserRedeemed() ở đây — flag đó chỉ dành cho user
        // tự nhập key/token, không phải VIP cấp từ xem quảng cáo (audit F15).
        val success = AdManager.grantVipDays(this, REWARD_VIP_DAYS)
        if (success) {
            showActivationSuccess(daysGranted = REWARD_VIP_DAYS)
        } else {
            showActivationFailed()
        }
    }

    private fun bindUi() {
        val isVip = AdManager.isVipByKeyActive()
        val expiryMs = AdManager.getVipByKeyExpiry()
        // Single source of truth = SDK (audit F14) — tự lưu riêng sẽ sai khi VIP cấp qua auto-trial/
        // grantVipDays mà VipPrefs không biết.
        val grantedAtMs = AdManager.getVipGrantedAtMs()

        if (isVip && expiryMs > System.currentTimeMillis()) {
            // Active VIP state
            binding.layoutStatusHeaderBackground.setBackgroundResource(R.drawable.bg_vip_status_header_active)
            binding.tvStatusTitle.text = getString(R.string.vip_active)

            val formattedExpiry = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(expiryMs))
            binding.tvStatusSubtitle.text = getString(R.string.vip_until, formattedExpiry)
            binding.tvStatusSubtitle.visibility = View.VISIBLE

            binding.progressVip.visibility = View.VISIBLE
            binding.tvCountdown.visibility = View.VISIBLE
            binding.cardVipDetails.visibility = View.VISIBLE

            // Disable Watch Ad button when VIP is active
            binding.btnWatchAd.isEnabled = false

            // Determine if first install grace VIP or key redeemed
            val isFirstInstallGrace = AppPreferences.getInstance(this).isAddVIPMemberFirstInitSuccess() &&
                    !vipPrefs.userRedeemedAtLeastOnce()

            if (isFirstInstallGrace) {
                binding.tvActiveVipLabel.text = getString(R.string.vip_entry_first_install)
            } else {
                // Compute or guess days based on duration
                val durationDays = Math.ceil((expiryMs - grantedAtMs).toDouble() / (24 * 3600 * 1000)).toInt()
                binding.tvActiveVipLabel.text = getString(R.string.vip_entry_redeemed, durationDays)
            }

            val formattedGranted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(grantedAtMs))
            binding.tvActivatedAt.text = getString(R.string.vip_activated_at, formattedGranted)
            binding.tvExpiresAt.text = getString(R.string.vip_expires_at, formattedExpiry)

            startCountdown(grantedAtMs, expiryMs)
        } else {
            // Free user state
            binding.layoutStatusHeaderBackground.setBackgroundResource(R.drawable.bg_vip_status_header_free)
            binding.tvStatusTitle.text = getString(R.string.vip_free_user)
            binding.tvStatusSubtitle.visibility = View.GONE
            binding.progressVip.visibility = View.GONE
            binding.tvCountdown.visibility = View.GONE
            binding.cardVipDetails.visibility = View.GONE

            // Enable Watch Ad button when VIP is not active
            binding.btnWatchAd.isEnabled = true

            stopCountdown()
        }
    }

    // Progress computation helper (elapsed semantic: empty at activation, full at expiry)
    private fun computeElapsedProgress(grantedAtMs: Long, expiresAtMs: Long, nowMs: Long): Int {
        val total = expiresAtMs - grantedAtMs
        if (total <= 0L) return 100
        val elapsed = nowMs - grantedAtMs
        return ((elapsed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }

    private fun startCountdown(grantedAtMs: Long, expiryMs: Long) {
        stopCountdown()
        val remainingMs = expiryMs - System.currentTimeMillis()
        if (remainingMs <= 0) {
            bindUi()
            return
        }

        countDownTimer = object : CountDownTimer(remainingMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val now = System.currentTimeMillis()
                
                // Update Progress indicator
                val progress = computeElapsedProgress(grantedAtMs, expiryMs, now)
                binding.progressVip.setProgressCompat(progress, true)

                // Update Countdown text
                val days = millisUntilFinished / (24 * 3600 * 1000)
                val hours = (millisUntilFinished % (24 * 3600 * 1000)) / (3600 * 1000)
                val minutes = (millisUntilFinished % (3600 * 1000)) / (60 * 1000)
                val seconds = (millisUntilFinished % (60 * 1000)) / 1000

                binding.tvCountdown.text = getString(
                    R.string.vip_remaining,
                    days.toInt(),
                    hours.toInt(),
                    minutes.toInt(),
                    seconds.toInt()
                )

                // Crown count-up/animate when minute changes
                val currentMin = minutes.toInt()
                if (lastMinute != null && lastMinute != currentMin) {
                    triggerCountUpAnimation(lastMinute!!, currentMin)
                }
                lastMinute = currentMin
            }

            override fun onFinish() {
                bindUi()
            }
        }.start()
    }

    private fun stopCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    // Animation 1: Pulse animation for Watch Ad button
    private fun startLoopAnimations() {
        cancelLoopAnimations()

        // Pulse watch ad button
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.btnWatchAd,
            PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.04f),
            PropertyValuesHolder.ofFloat("scaleY", 1.0f, 1.04f)
        ).apply {
            duration = 1200L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Crown shimmer/rotation animation
        crownShimmerAnimator = ObjectAnimator.ofFloat(
            binding.imgCrown,
            "rotation",
            -8f,
            8f
        ).apply {
            duration = 2000L
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun cancelLoopAnimations() {
        pulseAnimator?.cancel()
        pulseAnimator = null

        crownShimmerAnimator?.cancel()
        crownShimmerAnimator = null
    }

    // Animation 2: Slide-in from below for UI card elements on launch
    private fun triggerSlideInAnimation() {
        slideInAnimator?.cancel()
        
        binding.cardStatusHeader.translationY = 400f
        binding.cardStatusHeader.alpha = 0f
        binding.cardActivation.translationY = 600f
        binding.cardActivation.alpha = 0f
        binding.btnWatchAd.translationY = 800f
        binding.btnWatchAd.alpha = 0f

        slideInAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                
                // Status header animates in the 0.0 - 0.5 fraction
                val f1 = (fraction / 0.5f).coerceIn(0f, 1f)
                binding.cardStatusHeader.translationY = (1f - f1) * 400f
                binding.cardStatusHeader.alpha = f1

                // Activation card animates in the 0.2 - 0.8 fraction
                val f2 = ((fraction - 0.2f) / 0.6f).coerceIn(0f, 1f)
                binding.cardActivation.translationY = (1f - f2) * 600f
                binding.cardActivation.alpha = f2

                // Watch ad button animates in the 0.4 - 1.0 fraction
                val f3 = ((fraction - 0.4f) / 0.6f).coerceIn(0f, 1f)
                binding.btnWatchAd.translationY = (1f - f3) * 800f
                binding.btnWatchAd.alpha = f3
            }
            start()
        }
    }

    // Animation 4: Value count up/down when minute changes
    private fun triggerCountUpAnimation(from: Int, to: Int) {
        countUpAnimator?.cancel()
        countUpAnimator = ValueAnimator.ofInt(from, to).apply {
            duration = 600L
            addUpdateListener { animator ->
                // Small bounce scale effect during minute change
                val scale = 1f + (animator.animatedFraction * 0.1f)
                binding.tvCountdown.scaleX = scale
                binding.tvCountdown.scaleY = scale
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tvCountdown.scaleX = 1.0f
                    binding.tvCountdown.scaleY = 1.0f
                }
            })
            start()
        }
    }

    // Animation 5: Confetti + Haptic on Success
    private fun triggerSuccessEffect() {
        // Confetti
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            angle = 270,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726f, 0x96c3ec, 0xffd700),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
        binding.viewKonfetti.start(party)

        // Haptic feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    }

    /**
     * @param daysGranted Số ngày CHÍNH XÁC vừa cấp, nếu caller biết chắc (vd reward = luôn đúng 3).
     * Để `null` khi không biết trước (redeem code/token) — SDK CỘNG DỒN vào hạn hiện có và ghi đè
     * `grantedAtMs` bằng thời điểm activate hiện tại (audit smoke2 #2, codex + claude2 cùng xác nhận
     * qua source `AppPreferences.kt`), nên back-calculate `expiry − grantedAtMs` sẽ SAI (hiện tổng số
     * ngày còn lại thay vì số ngày vừa cộng) nếu user redeem lúc đang có VIP còn hạn — dùng ngày hết
     * hạn thay vì đoán số ngày.
     */
    private fun showActivationSuccess(daysGranted: Int? = null) {
        triggerSuccessEffect()
        // Clear VIP Key edit text
        binding.etVipKey.text?.clear()
        bindUi()
        val message = if (daysGranted != null) {
            getString(R.string.vip_success_message, daysGranted)
        } else {
            val formattedExpiry = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(AdManager.getVipByKeyExpiry()))
            getString(R.string.vip_success_message_until, formattedExpiry)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_success_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showActivationFailed() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_failed_title)
            .setMessage(R.string.vip_failed_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /** Shown when no rewarded/interstitial ad is available (offline or no-fill). */
    private fun showNoAdDialog() {
        if (isFinishing) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_no_ad_title)
            .setMessage(R.string.vip_no_ad_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    /**
     * Shown when the Interstitial fallback ran (rewarded unavailable) but — correctly — granted NO
     * VIP reward. Non-rewarded ad formats must never credit a reward (Google/AppLovin policy).
     */
    private fun showNoRewardDialog() {
        if (isFinishing) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_no_reward_title)
            .setMessage(R.string.vip_no_reward_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroy() {
        activateRunnable?.let { binding.root.removeCallbacks(it) }
        activateRunnable = null
        // Chống WindowLeaked nếu user thoát màn hình trong lúc dialog verifying đang chờ
        // activateRunnable chạy (audit smoke2 #3).
        activateProgressDialog?.dismiss()
        activateProgressDialog = null
        stopCountdown()
        cancelLoopAnimations()
        slideInAnimator?.cancel()
        slideInAnimator = null
        countUpAnimator?.cancel()
        countUpAnimator = null
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}

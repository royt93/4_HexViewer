package com.galaxyjoy.hexviewer.ads;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Testable, exactly-once orchestration for the asynchronous consent → splash-ad → navigation flow.
 * The SDK owns its watchdogs; this coordinator deliberately has no app-side timeout.
 */
public final class SplashAdCoordinator {
    public interface ConsentCallback { void onResult(boolean canRequestAds); }
    public interface ConsentRequester { void request(ConsentCallback callback); }
    public interface SplashRunner { void run(Runnable onComplete); }
    public interface AliveChecker { boolean isAlive(); }

    private final ConsentRequester consentRequester;
    private final SplashRunner splashRunner;
    private final Runnable navigator;
    private final AliveChecker aliveChecker;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean navigationStarted = new AtomicBoolean(false);
    private final AtomicBoolean consentHandled = new AtomicBoolean(false);

    public SplashAdCoordinator(
            ConsentRequester consentRequester,
            SplashRunner splashRunner,
            Runnable navigator,
            AliveChecker aliveChecker
    ) {
        this.consentRequester = consentRequester;
        this.splashRunner = splashRunner;
        this.navigator = navigator;
        this.aliveChecker = aliveChecker;
    }

    public void start(boolean online) {
        if (!started.compareAndSet(false, true)) return;
        if (!online) {
            navigateOnce();
            return;
        }
        consentRequester.request(canRequestAds -> {
            if (!aliveChecker.isAlive() || !consentHandled.compareAndSet(false, true)) return;
            if (canRequestAds) {
                splashRunner.run(this::navigateOnce);
            } else {
                navigateOnce();
            }
        });
    }

    private void navigateOnce() {
        if (aliveChecker.isAlive() && navigationStarted.compareAndSet(false, true)) {
            navigator.run();
        }
    }
}

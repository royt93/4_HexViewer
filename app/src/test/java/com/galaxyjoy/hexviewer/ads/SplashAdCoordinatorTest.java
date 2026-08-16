package com.galaxyjoy.hexviewer.ads;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

public class SplashAdCoordinatorTest {
    private SplashAdCoordinator.ConsentCallback consentCallback;
    private Runnable splashComplete;
    private final AtomicInteger consentRequests = new AtomicInteger();
    private final AtomicInteger splashRuns = new AtomicInteger();
    private final AtomicInteger navigations = new AtomicInteger();
    private final AtomicBoolean alive = new AtomicBoolean(true);
    private SplashAdCoordinator coordinator;

    @Before
    public void setUp() {
        consentCallback = null;
        splashComplete = null;
        consentRequests.set(0);
        splashRuns.set(0);
        navigations.set(0);
        alive.set(true);
        coordinator = new SplashAdCoordinator(
                callback -> {
                    consentRequests.incrementAndGet();
                    consentCallback = callback;
                },
                onComplete -> {
                    splashRuns.incrementAndGet();
                    splashComplete = onComplete;
                },
                navigations::incrementAndGet,
                alive::get
        );
    }

    @Test
    public void offline_navigatesImmediately_withoutConsentOrAd() {
        coordinator.start(false);
        assertEquals(0, consentRequests.get());
        assertEquals(0, splashRuns.get());
        assertEquals(1, navigations.get());
    }

    @Test
    public void online_waitsForAsyncConsent_withoutAppTimeout() {
        coordinator.start(true);
        assertEquals(1, consentRequests.get());
        assertEquals(0, splashRuns.get());
        assertEquals(0, navigations.get());
        assertNotNull(consentCallback);
    }

    @Test
    public void consentDenied_navigatesWithoutSplashAd() {
        coordinator.start(true);
        consentCallback.onResult(false);
        assertEquals(0, splashRuns.get());
        assertEquals(1, navigations.get());
    }

    @Test
    public void consentAllowed_waitsForAsyncSplashCompletion() {
        coordinator.start(true);
        consentCallback.onResult(true);
        assertEquals(1, splashRuns.get());
        assertEquals(0, navigations.get());
        assertNotNull(splashComplete);
        splashComplete.run();
        assertEquals(1, navigations.get());
    }

    @Test
    public void duplicateConsentCallback_isHandledExactlyOnce() {
        coordinator.start(true);
        consentCallback.onResult(true);
        consentCallback.onResult(false);
        assertEquals(1, splashRuns.get());
        assertEquals(0, navigations.get());
        splashComplete.run();
        splashComplete.run();
        assertEquals(1, navigations.get());
    }

    @Test
    public void consentCallbackAfterActivityDestroyed_isIgnored() {
        coordinator.start(true);
        alive.set(false);
        consentCallback.onResult(true);
        assertEquals(0, splashRuns.get());
        assertEquals(0, navigations.get());
    }

    @Test
    public void splashCompletionAfterActivityDestroyed_isIgnored() {
        coordinator.start(true);
        consentCallback.onResult(true);
        alive.set(false);
        splashComplete.run();
        assertEquals(0, navigations.get());
    }
}

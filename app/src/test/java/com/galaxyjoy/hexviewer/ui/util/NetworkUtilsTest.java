package com.galaxyjoy.hexviewer.ui.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NetworkUtilsTest {

    // --- helpers ---

    private Context ctxWith(NetworkCapabilities caps) {
        ConnectivityManager cm = mock(ConnectivityManager.class);
        android.net.Network net = mock(android.net.Network.class);
        when(cm.getActiveNetwork()).thenReturn(net);
        when(cm.getNetworkCapabilities(net)).thenReturn(caps);
        Context ctx = mock(Context.class);
        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        return ctx;
    }

    private NetworkCapabilities capsWithTransport(int transport) {
        NetworkCapabilities caps = mock(NetworkCapabilities.class);
        when(caps.hasTransport(transport)).thenReturn(true);
        return caps;
    }

    // --- tests ---

    @Test
    public void nullConnectivityManager_returnsFalse() {
        Context ctx = mock(Context.class);
        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null);
        assertFalse(NetworkUtils.isNetworkAvailable(ctx));
    }

    @Test
    public void noActiveNetwork_returnsFalse() {
        ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getActiveNetwork()).thenReturn(null);
        Context ctx = mock(Context.class);
        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        assertFalse(NetworkUtils.isNetworkAvailable(ctx));
    }

    @Test
    public void nullNetworkCapabilities_returnsFalse() {
        ConnectivityManager cm = mock(ConnectivityManager.class);
        android.net.Network net = mock(android.net.Network.class);
        when(cm.getActiveNetwork()).thenReturn(net);
        when(cm.getNetworkCapabilities(net)).thenReturn(null);
        Context ctx = mock(Context.class);
        when(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(cm);
        assertFalse(NetworkUtils.isNetworkAvailable(ctx));
    }

    @Test
    public void wifiTransport_returnsTrue() {
        assertTrue(NetworkUtils.isNetworkAvailable(
                ctxWith(capsWithTransport(NetworkCapabilities.TRANSPORT_WIFI))));
    }

    @Test
    public void cellularTransport_returnsTrue() {
        assertTrue(NetworkUtils.isNetworkAvailable(
                ctxWith(capsWithTransport(NetworkCapabilities.TRANSPORT_CELLULAR))));
    }

    @Test
    public void ethernetTransport_returnsTrue() {
        assertTrue(NetworkUtils.isNetworkAvailable(
                ctxWith(capsWithTransport(NetworkCapabilities.TRANSPORT_ETHERNET))));
    }

    @Test
    public void noMatchingTransport_returnsFalse() {
        NetworkCapabilities caps = mock(NetworkCapabilities.class);
        // hasTransport returns false by default for all
        assertFalse(NetworkUtils.isNetworkAvailable(ctxWith(caps)));
    }
}

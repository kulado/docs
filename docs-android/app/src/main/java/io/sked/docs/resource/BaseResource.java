package io.sked.docs.resource;

import android.content.Context;

import io.sked.docs.util.PreferenceUtil;

/**
 * Base class for API access.
 * 
 * @author bgamard
 */
public class BaseResource {
    /**
     * Returns cleaned API URL.
     *
     * @param context Context
     * @return Cleaned API URL
     */
    protected static String getApiUrl(Context context) {
        String serverUrl = PreferenceUtil.getServerUrl(context);
        
        if (serverUrl == null) {
            return null;
        }
        
        return serverUrl + "/api";
    }
}

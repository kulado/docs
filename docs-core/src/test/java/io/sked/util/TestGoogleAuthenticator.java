package io.sked.util;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import io.sked.util.totp.GoogleAuthenticator;
import io.sked.util.totp.GoogleAuthenticatorKey;

/**
 * Test of {@link GoogleAuthenticator}
 * 
 * @author bgamard
 */
public class TestGoogleAuthenticator {
    @Test
    public void testGoogleAuthenticator() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        Assert.assertNotNull(key.getVerificationCode());
        Assert.assertEquals(5, key.getScratchCodes().size());
        int validationCode = gAuth.calculateCode(key.getKey(), new Date().getTime() / 30000);
        Assert.assertTrue(gAuth.authorize(key.getKey(), validationCode));
    }
}

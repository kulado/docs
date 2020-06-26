package io.sked.docs.rest.util;

import io.sked.rest.exception.ClientException;
import io.sked.rest.util.ValidationUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the validations.
 *
 * @author jtremeaux 
 */
public class TestValidationUtil {
    @Test
    public void testValidateHttpUrlFail() throws Exception {
        ValidationUtil.validateHttpUrl("http://www.google.com", "url");
        ValidationUtil.validateHttpUrl("https://www.google.com", "url");
        ValidationUtil.validateHttpUrl(" https://www.google.com ", "url");
        try {
            ValidationUtil.validateHttpUrl("ftp://www.google.com", "url");
            Assert.fail();
        } catch (ClientException e) {
            // NOP
        }
        try {
            ValidationUtil.validateHttpUrl("http://", "url");
            Assert.fail();
        } catch (ClientException e) {
            // NOP
        }
    }
}

package io.sked.util;

import org.junit.Test;

import io.sked.util.css.Selector;

/**
 * Test of CSS utilities.
 * 
 * @author bgamard
 */
public class TestCss {
    @Test
    public void testBuildCss() {
        Selector selector = new Selector(".test")
            .rule("background-color", "yellow")
            .rule("font-family", "Comic Sans");
        System.out.println(selector);
    }
}

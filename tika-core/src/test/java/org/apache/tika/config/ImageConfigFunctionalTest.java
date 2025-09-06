/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.utils.ImageUtils;

/**
 * Comprehensive test demonstrating the ImageConfig functionality
 */
public class ImageConfigFunctionalTest {

    @Test
    public void testParseContextIntegration() {
        // Test how ImageConfig integrates with ParseContext
        
        // Test 1: Default behavior (no config set)
        ParseContext defaultContext = new ParseContext();
        ImageConfig config = defaultContext.get(ImageConfig.class);
        assertTrue(config == null, "No config should be set by default");
        
        // Test 2: Setting and retrieving config
        ParseContext configuredContext = new ParseContext();
        configuredContext.set(ImageConfig.class, ImageConfig.BASE64);
        ImageConfig retrievedConfig = configuredContext.get(ImageConfig.class);
        assertTrue(retrievedConfig != null);
        assertTrue(retrievedConfig.isConvertEmbeddedToBase64());
        
        // Test 3: Multiple contexts with different configs
        ParseContext context1 = new ParseContext();
        context1.set(ImageConfig.class, ImageConfig.DEFAULT);
        
        ParseContext context2 = new ParseContext();  
        context2.set(ImageConfig.class, ImageConfig.BASE64);
        
        assertTrue(!context1.get(ImageConfig.class).isConvertEmbeddedToBase64());
        assertTrue(context2.get(ImageConfig.class).isConvertEmbeddedToBase64());
    }

    @Test
    public void testImageUtilsAllScenarios() {
        // Test various scenarios with ImageUtils
        
        // Scenario 1: No config (null)
        AttributesImpl attrs1 = ImageUtils.createImageAttributes(
            null, null, "test.png", "image/png", "alt text");
        assertTrue(attrs1.getValue("src").equals("embedded:test.png"));
        
        // Scenario 2: Default config, no data
        AttributesImpl attrs2 = ImageUtils.createImageAttributes(
            ImageConfig.DEFAULT, null, "test.png", "image/png", "alt text");
        assertTrue(attrs2.getValue("src").equals("embedded:test.png"));
        
        // Scenario 3: Base64 config, no data (fallback to embedded)
        AttributesImpl attrs3 = ImageUtils.createImageAttributes(
            ImageConfig.BASE64, null, "test.png", "image/png", "alt text");
        assertTrue(attrs3.getValue("src").equals("embedded:test.png"));
        
        // Scenario 4: Base64 config with data
        byte[] imageData = "test image data".getBytes(StandardCharsets.UTF_8);
        AttributesImpl attrs4 = ImageUtils.createImageAttributes(
            ImageConfig.BASE64, imageData, "test.png", "image/png", "alt text");
        assertTrue(attrs4.getValue("src").startsWith("data:image/png;base64,"));
        
        // Scenario 5: Base64 config with data but no MIME type (should guess from filename)
        AttributesImpl attrs5 = ImageUtils.createImageAttributes(
            ImageConfig.BASE64, imageData, "test.jpg", null, "alt text");
        assertTrue(attrs5.getValue("src").startsWith("data:image/jpeg;base64,"));
        
        // Scenario 6: Verify alt text handling
        AttributesImpl attrs6 = ImageUtils.createImageAttributes(
            ImageConfig.DEFAULT, null, "test.png", "image/png", null);
        assertTrue(attrs6.getValue("alt").equals("test.png"), "Should use filename as alt when alt text is null");
    }

    @Test
    public void testConfigurationPersistence() {
        // Test that configuration persists correctly in ParseContext
        ParseContext context = new ParseContext();
        
        // Initially no config
        assertTrue(context.get(ImageConfig.class) == null);
        
        // Set config
        ImageConfig originalConfig = new ImageConfig(true);
        context.set(ImageConfig.class, originalConfig);
        
        // Retrieve and verify
        ImageConfig retrievedConfig = context.get(ImageConfig.class);
        assertTrue(retrievedConfig == originalConfig, "Should be the same object");
        assertTrue(retrievedConfig.isConvertEmbeddedToBase64());
        
        // Override with different config
        context.set(ImageConfig.class, ImageConfig.DEFAULT);
        ImageConfig newConfig = context.get(ImageConfig.class);
        assertTrue(!newConfig.isConvertEmbeddedToBase64());
        assertTrue(newConfig != originalConfig, "Should be different object");
    }
}
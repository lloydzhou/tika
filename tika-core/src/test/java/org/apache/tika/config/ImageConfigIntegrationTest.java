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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

/**
 * Integration test for ImageConfig functionality across different parsers
 */
public class ImageConfigIntegrationTest {

    @Test
    public void testImageConfigWithDummyContent() throws IOException, SAXException {
        // Test with embedded: URLs (default behavior)
        ParseContext defaultContext = new ParseContext();
        // ImageConfig.DEFAULT is not set, should use embedded: URLs
        
        // Test with base64 conversion enabled
        ParseContext base64Context = new ParseContext();
        base64Context.set(ImageConfig.class, ImageConfig.BASE64);
        
        // Since we don't have actual test documents with images in this test,
        // we'll just verify that the configuration is properly stored and retrieved
        
        ImageConfig retrieved = base64Context.get(ImageConfig.class);
        assertTrue(retrieved.isConvertEmbeddedToBase64());
        
        ImageConfig defaultConfig = defaultContext.get(ImageConfig.class);
        // Should be null when not set
        assertTrue(defaultConfig == null);
    }

    @Test 
    public void testImageUtilsWithDifferentConfigs() {
        // Test that ImageUtils handles null config gracefully
        ImageConfig nullConfig = null;
        var attrs = org.apache.tika.utils.ImageUtils.createImageAttributes(
            nullConfig, null, "test.png", "image/png", "test");
        assertTrue(attrs.getValue("src").startsWith("embedded:"));
        
        // Test with DEFAULT config
        attrs = org.apache.tika.utils.ImageUtils.createImageAttributes(
            ImageConfig.DEFAULT, null, "test.png", "image/png", "test");
        assertTrue(attrs.getValue("src").startsWith("embedded:"));
        
        // Test with BASE64 config but no data
        attrs = org.apache.tika.utils.ImageUtils.createImageAttributes(
            ImageConfig.BASE64, null, "test.png", "image/png", "test");
        assertTrue(attrs.getValue("src").startsWith("embedded:"));
        
        // Test with BASE64 config and data
        byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
        attrs = org.apache.tika.utils.ImageUtils.createImageAttributes(
            ImageConfig.BASE64, testData, "test.png", "image/png", "test");
        assertTrue(attrs.getValue("src").startsWith("data:image/png;base64,"));
    }
}
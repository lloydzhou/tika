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
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.config.ImageConfig;

/**
 * Test cases for ImageUtils functionality
 */
public class ImageUtilsTest {

    @Test
    public void testGuessMimeTypeFromFilename() {
        assertEquals("image/jpeg", ImageUtils.guessMimeTypeFromFilename("image.jpg"));
        assertEquals("image/jpeg", ImageUtils.guessMimeTypeFromFilename("image.jpeg"));
        assertEquals("image/png", ImageUtils.guessMimeTypeFromFilename("image.png"));
        assertEquals("image/gif", ImageUtils.guessMimeTypeFromFilename("image.gif"));
        assertEquals("image/bmp", ImageUtils.guessMimeTypeFromFilename("image.bmp"));
        assertEquals("image/tiff", ImageUtils.guessMimeTypeFromFilename("image.tiff"));
        assertEquals("image/tiff", ImageUtils.guessMimeTypeFromFilename("image.tif"));
        assertEquals("image/webp", ImageUtils.guessMimeTypeFromFilename("image.webp"));
        assertEquals("image/svg+xml", ImageUtils.guessMimeTypeFromFilename("image.svg"));
        assertEquals("image/unknown", ImageUtils.guessMimeTypeFromFilename("image.unknown"));
        assertNull(ImageUtils.guessMimeTypeFromFilename(null));
    }

    @Test
    public void testCreateImageAttributesDefault() {
        ImageConfig config = ImageConfig.DEFAULT;
        AttributesImpl attrs = ImageUtils.createImageAttributes(config, null, "test.png", "image/png", "Test Image");
        
        assertEquals("embedded:test.png", attrs.getValue("src"));
        assertEquals("Test Image", attrs.getValue("alt"));
    }

    @Test
    public void testCreateImageAttributesBase64() {
        ImageConfig config = ImageConfig.BASE64;
        byte[] imageData = "fake image data".getBytes();
        AttributesImpl attrs = ImageUtils.createImageAttributes(config, imageData, "test.png", "image/png", "Test Image");
        
        assertTrue(attrs.getValue("src").startsWith("data:image/png;base64,"));
        assertEquals("Test Image", attrs.getValue("alt"));
    }

    @Test
    public void testCreateImageAttributesBase64NoData() {
        ImageConfig config = ImageConfig.BASE64;
        AttributesImpl attrs = ImageUtils.createImageAttributes(config, null, "test.png", "image/png", "Test Image");
        
        // Should fall back to embedded: when no image data available
        assertEquals("embedded:test.png", attrs.getValue("src"));
        assertEquals("Test Image", attrs.getValue("alt"));
    }

    @Test
    public void testCreateImageAttributesWithFilenameAlt() {
        ImageConfig config = ImageConfig.DEFAULT;
        AttributesImpl attrs = ImageUtils.createImageAttributes(config, null, "test.png", "image/png", null);
        
        assertEquals("embedded:test.png", attrs.getValue("src"));
        assertEquals("test.png", attrs.getValue("alt"));
    }
}
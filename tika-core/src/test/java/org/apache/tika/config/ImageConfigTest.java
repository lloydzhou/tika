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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test cases for ImageConfig functionality
 */
public class ImageConfigTest {

    @Test
    public void testDefaultConfig() {
        ImageConfig config = new ImageConfig();
        assertFalse(config.isConvertEmbeddedToBase64());
    }

    @Test
    public void testBase64Config() {
        ImageConfig config = new ImageConfig(true);
        assertTrue(config.isConvertEmbeddedToBase64());
    }

    @Test
    public void testStaticConfigs() {
        assertFalse(ImageConfig.DEFAULT.isConvertEmbeddedToBase64());
        assertTrue(ImageConfig.BASE64.isConvertEmbeddedToBase64());
    }

    @Test
    public void testSetterAndGetter() {
        ImageConfig config = new ImageConfig();
        config.setConvertEmbeddedToBase64(true);
        assertTrue(config.isConvertEmbeddedToBase64());
        
        config.setConvertEmbeddedToBase64(false);
        assertFalse(config.isConvertEmbeddedToBase64());
    }

    @Test
    public void testEqualsAndHashCode() {
        ImageConfig config1 = new ImageConfig(true);
        ImageConfig config2 = new ImageConfig(true);
        ImageConfig config3 = new ImageConfig(false);
        
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        
        assertFalse(config1.equals(config3));
    }

    @Test
    public void testToString() {
        ImageConfig config = new ImageConfig(true);
        assertTrue(config.toString().contains("convertEmbeddedToBase64=true"));
    }
}
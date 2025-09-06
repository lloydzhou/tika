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

import java.io.Serializable;

/**
 * Configuration class for image handling in Tika parsers.
 * Controls how embedded images are output in HTML format.
 */
public class ImageConfig implements Serializable {

    private static final long serialVersionUID = -8629833163423391866L;

    /**
     * Default configuration that uses embedded: references (traditional behavior)
     */
    public static final ImageConfig DEFAULT = new ImageConfig(false);

    /**
     * Configuration that converts embedded: references to base64 data URLs
     */
    public static final ImageConfig BASE64 = new ImageConfig(true);

    /**
     * Whether to convert embedded: image references to base64 data URLs
     */
    private boolean convertEmbeddedToBase64 = false;

    /**
     * Create an ImageConfig with default settings (embedded: references)
     */
    public ImageConfig() {
        this(false);
    }

    /**
     * Create an ImageConfig with specified settings
     * 
     * @param convertEmbeddedToBase64 whether to convert embedded: URLs to base64 data URLs
     */
    public ImageConfig(boolean convertEmbeddedToBase64) {
        this.convertEmbeddedToBase64 = convertEmbeddedToBase64;
    }

    /**
     * @return true if embedded: image references should be converted to base64 data URLs
     */
    public boolean isConvertEmbeddedToBase64() {
        return convertEmbeddedToBase64;
    }

    /**
     * Set whether embedded: image references should be converted to base64 data URLs
     * 
     * @param convertEmbeddedToBase64 true to convert to base64, false to keep embedded: URLs
     */
    public void setConvertEmbeddedToBase64(boolean convertEmbeddedToBase64) {
        this.convertEmbeddedToBase64 = convertEmbeddedToBase64;
    }

    @Override
    public String toString() {
        return "ImageConfig{convertEmbeddedToBase64=" + convertEmbeddedToBase64 + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageConfig that = (ImageConfig) o;
        return convertEmbeddedToBase64 == that.convertEmbeddedToBase64;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(convertEmbeddedToBase64);
    }
}
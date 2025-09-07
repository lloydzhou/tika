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

import java.util.Base64;
import java.util.Locale;

/**
 * Utility class for handling embedded image references in HTML output.
 * Provides methods to convert embedded: URLs to base64 data URLs when configured to do so.
 */
public class ImageUtils {
    /**
     * Guess MIME type from filename extension.
     * 
     * @param filename the filename
     * @return the guessed MIME type, or null if cannot determine
     */
    public static String guessMimeTypeFromFilename(String filename) {
        if (filename == null) {
            return null;
        }
        
        String extension = filename.toLowerCase(Locale.ROOT);
        
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".bmp")) {
            return "image/bmp";
        } else if (extension.endsWith(".tiff") || extension.endsWith(".tif")) {
            return "image/tiff";
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        } else if (extension.endsWith(".svg")) {
            return "image/svg+xml";
        }
        
        return "image/" + getFileExtension(filename);
    }

    /**
     * Extract file extension from filename.
     * 
     * @param filename the filename
     * @return the extension without the dot, or empty string if no extension
     */
    private static String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}

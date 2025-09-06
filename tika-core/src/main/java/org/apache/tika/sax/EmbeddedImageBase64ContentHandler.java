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

package org.apache.tika.sax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.EmbeddedDocumentUtil;
import org.apache.commons.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;

/**
 * Content handler that converts embedded image URLs (embedded:filename.png) 
 * to base64 data URLs in HTML output by intercepting embedded document extraction.
 */
public class EmbeddedImageBase64ContentHandler extends ContentHandlerDecorator {
    
    private final Map<String, byte[]> imageCache = new HashMap<>();
    private final ParseContext parseContext;
    private final StringBuilder htmlBuffer = new StringBuilder();
    private boolean isBuffering = false;
    
    public EmbeddedImageBase64ContentHandler(ContentHandler handler, ParseContext parseContext) {
        super(handler);
        this.parseContext = parseContext;
        
        // Set up our custom embedded document extractor to capture images
        EmbeddedDocumentExtractor originalExtractor = 
            EmbeddedDocumentUtil.getEmbeddedDocumentExtractor(parseContext);
        
        ImageCapturingExtractor imageExtractor = new ImageCapturingExtractor(originalExtractor, imageCache);
        parseContext.set(EmbeddedDocumentExtractor.class, imageExtractor);
    }
    
    @Override
    public void endDocument() throws SAXException {
        // At end of document, process any buffered content and convert embedded: URLs
        if (htmlBuffer.length() > 0) {
            String content = htmlBuffer.toString();
            content = convertEmbeddedUrls(content);
            
            // Output the processed content as characters
            char[] chars = content.toCharArray();
            super.characters(chars, 0, chars.length);
        }
        
        super.endDocument();
    }
    
    private String convertEmbeddedUrls(String content) {
        // Convert embedded: URLs to base64 data URLs using simple string replacement
        String result = content;
        
        for (Map.Entry<String, byte[]> entry : imageCache.entrySet()) {
            String filename = entry.getKey();
            byte[] imageData = entry.getValue();
            
            String embeddedUrl = "src=\"embedded:" + filename + "\"";
            if (result.contains(embeddedUrl)) {
                String mimeType = getMimeTypeFromFilename(filename);
                String base64 = Base64.getEncoder().encodeToString(imageData);
                String dataUrl = "src=\"data:" + mimeType + ";base64," + base64 + "\"";
                result = result.replace(embeddedUrl, dataUrl);
            }
        }
        
        return result;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) 
            throws SAXException {
        
        if ("img".equals(localName) && XHTMLContentHandler.XHTML.equals(uri)) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            
            for (int i = 0; i < newAttrs.getLength(); i++) {
                if ("src".equals(newAttrs.getLocalName(i))) {
                    String src = newAttrs.getValue(i);
                    if (src != null && src.startsWith("embedded:")) {
                        String filename = src.substring("embedded:".length());
                        String base64Url = convertToBase64DataUrl(filename);
                        if (base64Url != null) {
                            newAttrs.setValue(i, base64Url);
                        }
                    }
                }
            }
            super.startElement(uri, localName, qName, newAttrs);
        } else {
            super.startElement(uri, localName, qName, atts);
        }
    }
    
    private String convertToBase64DataUrl(String filename) {
        byte[] imageData = imageCache.get(filename);
        
        if (imageData != null) {
            String mimeType = getMimeTypeFromFilename(filename);
            String base64 = Base64.getEncoder().encodeToString(imageData);
            return "data:" + mimeType + ";base64," + base64;
        }
        
        return null; // Return null to keep original embedded: URL
    }
    
    private String getMimeTypeFromFilename(String filename) {
        String extension = filename.toLowerCase(java.util.Locale.ROOT);
        
        if (extension.endsWith(".png")) {
            return "image/png";
        } else if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (extension.endsWith(".gif")) {
            return "image/gif";
        } else if (extension.endsWith(".bmp")) {
            return "image/bmp";
        } else if (extension.endsWith(".tif") || extension.endsWith(".tiff")) {
            return "image/tiff";
        } else if (extension.endsWith(".webp")) {
            return "image/webp";
        } else if (extension.endsWith(".svg")) {
            return "image/svg+xml";
        }
        
        return "image/png"; // Default fallback
    }
    
    /**
     * Custom embedded document extractor that captures image data
     */
    private static class ImageCapturingExtractor implements EmbeddedDocumentExtractor {
        private final EmbeddedDocumentExtractor delegate;
        private final Map<String, byte[]> imageCache;
        
        public ImageCapturingExtractor(EmbeddedDocumentExtractor delegate, Map<String, byte[]> imageCache) {
            this.delegate = delegate;
            this.imageCache = imageCache;
        }
        
        @Override
        public boolean shouldParseEmbedded(Metadata metadata) {
            return delegate.shouldParseEmbedded(metadata);
        }
        
        @Override
        public void parseEmbedded(TikaInputStream tis, ContentHandler handler, 
                                  Metadata metadata, boolean outputHtml) 
                throws SAXException, IOException {
            
            String name = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);
            String contentType = metadata.get(Metadata.CONTENT_TYPE);
            
            // If this is an image, capture its data before delegating
            if (name != null && contentType != null && contentType.startsWith("image/")) {
                try {
                    // Mark the stream so we can reset it
                    tis.mark(Integer.MAX_VALUE);
                    
                    // Read the image data
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(tis, baos);
                    byte[] imageBytes = baos.toByteArray();
                    imageCache.put(name, imageBytes);
                    
                    // Reset the stream for the delegate
                    tis.reset();
                } catch (Exception e) {
                    // If capturing fails, just continue with normal processing
                    System.err.println("Failed to capture image data for " + name + ": " + e.getMessage());
                }
            }
            
            // Delegate to the original extractor
            delegate.parseEmbedded(tis, handler, metadata, outputHtml);
        }
    }
}
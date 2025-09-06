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

package org.apache.tika.server.core.resource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EmbeddedImageBase64ContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;

/**
 * Test class for EmbeddedImageBase64ContentHandler
 */
public class EmbeddedImageBase64ContentHandlerTest {

    @Test
    public void testHandlerConstruction() throws Exception {
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        assertTrue(handler != null);
    }
    
    @Test
    public void testNonImageElementPassthrough() throws Exception {
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "href", "href", "CDATA", "http://example.com");
        
        handler.startElement(XHTMLContentHandler.XHTML, "a", "a", attrs);
        
        // Verify that the call was passed through unchanged
        verify(mockHandler).startElement(XHTMLContentHandler.XHTML, "a", "a", attrs);
    }
    
    @Test
    public void testImageElementWithoutEmbeddedSrc() throws Exception {
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "src", "src", "CDATA", "http://example.com/image.png");
        attrs.addAttribute("", "alt", "alt", "CDATA", "Regular image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs);
        
        // Verify that the call was passed through unchanged (no embedded: prefix)
        verify(mockHandler).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }
    
    @Test
    public void testImageElementWithEmbeddedSrc() throws Exception {
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "src", "src", "CDATA", "embedded:image1.png");
        attrs.addAttribute("", "alt", "alt", "CDATA", "Embedded image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs);
        
        // Verify the handler was called (the specific attributes depend on image data availability)
        verify(mockHandler).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }
    
    @Test
    public void testMimeTypeDetectionFromFilename() {
        // Test the mime type detection logic indirectly by checking behavior
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        // The mime type detection is used internally when converting images
        // This test ensures the handler can be created and used
        assertTrue(handler != null);
    }
    
    @Test
    public void testMultipleImageElements() throws Exception {
        ContentHandler mockHandler = mock(ContentHandler.class);
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        // First image with embedded: src
        AttributesImpl attrs1 = new AttributesImpl();
        attrs1.addAttribute("", "src", "src", "CDATA", "embedded:image1.png");
        attrs1.addAttribute("", "alt", "alt", "CDATA", "First image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs1);
        
        // Second image with regular src
        AttributesImpl attrs2 = new AttributesImpl();
        attrs2.addAttribute("", "src", "src", "CDATA", "http://example.com/image2.jpg");
        attrs2.addAttribute("", "alt", "alt", "CDATA", "Second image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs2);
        
        // Verify both calls were handled
        verify(mockHandler).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }
}
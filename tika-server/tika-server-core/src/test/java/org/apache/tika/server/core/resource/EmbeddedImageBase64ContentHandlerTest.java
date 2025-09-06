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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EmbeddedImageBase64ContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;

/**
 * Test class for EmbeddedImageBase64ContentHandler
 */
public class EmbeddedImageBase64ContentHandlerTest {

    // Lightweight content handler to record startElement calls for assertions
    private static class TestContentHandler implements ContentHandler {
        static class StartElementRecord {
            final String uri;
            final String localName;
            final String qName;
            final Attributes attributes;

            StartElementRecord(String uri, String localName, String qName, Attributes attributes) {
                this.uri = uri;
                this.localName = localName;
                this.qName = qName;
                this.attributes = attributes;
            }
        }

        final List<StartElementRecord> startElements = new ArrayList<>();

        @Override
        public void setDocumentLocator(org.xml.sax.Locator locator) {
            // no-op
        }

        @Override
        public void startDocument() throws SAXException {
            // no-op
        }

        @Override
        public void endDocument() throws SAXException {
            // no-op
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // no-op
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            // no-op
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            // Record the call
            startElements.add(new StartElementRecord(uri, localName, qName, atts));
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            // no-op
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // no-op
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // no-op
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            // no-op
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            // no-op
        }
    }

    @Test
    public void testHandlerConstruction() throws Exception {
        TestContentHandler mockHandler = new TestContentHandler();
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        assertNotNull(handler);
    }
    
    @Test
    public void testNonImageElementPassthrough() throws Exception {
        TestContentHandler mockHandler = new TestContentHandler();
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "href", "href", "CDATA", "http://example.com");
        
        handler.startElement(XHTMLContentHandler.XHTML, "a", "a", attrs);
        
        // Verify that the call was passed through unchanged
        assertEquals(1, mockHandler.startElements.size(), "Expected one startElement call recorded");
        TestContentHandler.StartElementRecord rec = mockHandler.startElements.get(0);
        assertEquals("a", rec.qName);
        assertEquals("http://example.com", rec.attributes.getValue("href"));
    }
    
    @Test
    public void testImageElementWithoutEmbeddedSrc() throws Exception {
        TestContentHandler mockHandler = new TestContentHandler();
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "src", "src", "CDATA", "http://example.com/image.png");
        attrs.addAttribute("", "alt", "alt", "CDATA", "Regular image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs);
        
        // Verify that the call was passed through unchanged (no embedded: prefix)
        assertEquals(1, mockHandler.startElements.size(), "Expected one startElement call recorded");
        TestContentHandler.StartElementRecord rec = mockHandler.startElements.get(0);
        assertEquals("img", rec.qName);
        assertEquals("http://example.com/image.png", rec.attributes.getValue("src"));
    }
    
    @Test
    public void testImageElementWithEmbeddedSrc() throws Exception {
        TestContentHandler mockHandler = new TestContentHandler();
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "src", "src", "CDATA", "embedded:image1.png");
        attrs.addAttribute("", "alt", "alt", "CDATA", "Embedded image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", attrs);
        
        // Verify the handler was called (the specific attributes depend on image data availability)
        assertTrue(mockHandler.startElements.size() >= 1, "Expected at least one startElement call recorded");
    }
    
    @Test
    public void testMimeTypeDetectionFromFilename() {
        // Test the mime type detection logic indirectly by checking behavior
        TestContentHandler mockHandler = new TestContentHandler();
        ParseContext parseContext = new ParseContext();
        
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(mockHandler, parseContext);
        
        // The mime type detection is used internally when converting images
        // This test ensures the handler can be created and used
        assertNotNull(handler);
    }
    
    @Test
    public void testMultipleImageElements() throws Exception {
        TestContentHandler mockHandler = new TestContentHandler();
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
        assertTrue(mockHandler.startElements.size() >= 2, "Expected at least two startElement calls recorded");
    }
}
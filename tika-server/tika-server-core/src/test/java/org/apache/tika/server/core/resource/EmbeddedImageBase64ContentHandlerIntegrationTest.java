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

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.EmbeddedImageBase64ContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.utils.XMLReaderUtils;

/**
 * Integration test for EmbeddedImageBase64ContentHandler with actual HTML output
 */
public class EmbeddedImageBase64ContentHandlerIntegrationTest {

    @Test
    public void testFullHTMLOutputPipeline() throws Exception {
        // Set up a complete HTML output pipeline similar to TikaResource
        StringWriter writer = new StringWriter();
        ParseContext parseContext = new ParseContext();
        
        // Create transformer handler for HTML output
        SAXTransformerFactory factory = XMLReaderUtils.getSAXTransformerFactory();
        TransformerHandler transformerHandler = factory.newTransformerHandler();
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.VERSION, "1.1");
        transformerHandler.setResult(new StreamResult(writer));
        
        // Create the full handler chain as used in TikaResource
        ContentHandler baseHandler = new ExpandedTitleContentHandler(transformerHandler);
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(baseHandler, parseContext);
        
        // Simulate HTML content generation
        handler.startDocument();
        handler.startElement(XHTMLContentHandler.XHTML, "html", "html", new AttributesImpl());
        handler.startElement(XHTMLContentHandler.XHTML, "body", "body", new AttributesImpl());
        
        // Add an img element with embedded: src
        AttributesImpl imgAttrs = new AttributesImpl();
        imgAttrs.addAttribute("", "src", "src", "CDATA", "embedded:test-image.png");
        imgAttrs.addAttribute("", "alt", "alt", "CDATA", "Test embedded image");
        
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", imgAttrs);
        handler.endElement(XHTMLContentHandler.XHTML, "img", "img");
        
        handler.endElement(XHTMLContentHandler.XHTML, "body", "body");
        handler.endElement(XHTMLContentHandler.XHTML, "html", "html");
        handler.endDocument();
        
        String output = writer.toString();
        
        // Verify that the output contains HTML
        assertTrue(output.contains("html"));
        assertTrue(output.contains("img"));
        
        // The image src will either be converted to base64 (if image data is available)
        // or remain as embedded: (if no image data is captured)
        assertTrue(output.contains("src=") && output.contains("test-image.png"));
        
        System.out.println("Generated HTML output:");
        System.out.println(output);
    }
    
    @Test
    public void testHandlerIntegrationWithMultipleImages() throws Exception {
        StringWriter writer = new StringWriter();
        ParseContext parseContext = new ParseContext();
        
        SAXTransformerFactory factory = XMLReaderUtils.getSAXTransformerFactory();
        TransformerHandler transformerHandler = factory.newTransformerHandler();
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        transformerHandler.setResult(new StreamResult(writer));
        
        ContentHandler baseHandler = new ExpandedTitleContentHandler(transformerHandler);
        EmbeddedImageBase64ContentHandler handler = 
            new EmbeddedImageBase64ContentHandler(baseHandler, parseContext);
        
        handler.startDocument();
        handler.startElement(XHTMLContentHandler.XHTML, "html", "html", new AttributesImpl());
        handler.startElement(XHTMLContentHandler.XHTML, "body", "body", new AttributesImpl());
        
        // Add multiple images - some embedded, some regular
        AttributesImpl img1Attrs = new AttributesImpl();
        img1Attrs.addAttribute("", "src", "src", "CDATA", "embedded:image1.png");
        img1Attrs.addAttribute("", "alt", "alt", "CDATA", "First embedded image");
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", img1Attrs);
        handler.endElement(XHTMLContentHandler.XHTML, "img", "img");
        
        AttributesImpl img2Attrs = new AttributesImpl();
        img2Attrs.addAttribute("", "src", "src", "CDATA", "http://example.com/regular.jpg");
        img2Attrs.addAttribute("", "alt", "alt", "CDATA", "Regular image");
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", img2Attrs);
        handler.endElement(XHTMLContentHandler.XHTML, "img", "img");
        
        AttributesImpl img3Attrs = new AttributesImpl();
        img3Attrs.addAttribute("", "src", "src", "CDATA", "embedded:image3.gif");
        img3Attrs.addAttribute("", "alt", "alt", "CDATA", "Third embedded image");
        handler.startElement(XHTMLContentHandler.XHTML, "img", "img", img3Attrs);
        handler.endElement(XHTMLContentHandler.XHTML, "img", "img");
        
        handler.endElement(XHTMLContentHandler.XHTML, "body", "body");
        handler.endElement(XHTMLContentHandler.XHTML, "html", "html");
        handler.endDocument();
        
        String output = writer.toString();
        
        // Verify all images are present
        assertTrue(output.contains("image1.png"));
        assertTrue(output.contains("regular.jpg"));
        assertTrue(output.contains("image3.gif"));
        
        // Regular image should remain unchanged
        assertTrue(output.contains("http://example.com/regular.jpg"));
        
        System.out.println("Multiple images HTML output:");
        System.out.println(output);
    }
}
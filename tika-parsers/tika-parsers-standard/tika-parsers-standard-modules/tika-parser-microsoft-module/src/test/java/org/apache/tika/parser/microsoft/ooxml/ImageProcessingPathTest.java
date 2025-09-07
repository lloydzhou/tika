package org.apache.tika.parser.microsoft.ooxml;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;

/**
 * Test to verify which code path is being used for image processing
 */
public class ImageProcessingPathTest extends TikaTest {

    @Test
    public void testEmbeddedUrlPath() throws Exception {
        System.out.println("\n=== Testing Embedded URL Path (convertEmbeddedImagesToBase64 = false) ===");
        
        // Test with embedded URLs (my embeddedPicRef method should be used)
        ParseContext context = new ParseContext();
        OfficeParserConfig config = new OfficeParserConfig();
        config.setConvertEmbeddedImagesToBase64(false);  // Use embedded: URLs
        context.set(OfficeParserConfig.class, config);
        
        XMLResult result = getXML("testWORD_embeded.docx", context);
        String xml = result.xml;
        
        boolean hasEmbeddedUrls = xml.contains("src=\"embedded:");
        boolean hasDataUrls = xml.contains("src=\"data:");
        
        System.out.println("Has embedded: URLs: " + hasEmbeddedUrls);
        System.out.println("Has data: URLs: " + hasDataUrls);
        
        // Show sample img tags
        String[] lines = xml.split("\n");
        for (String line : lines) {
            if (line.contains("<img") && line.contains("alt=")) {
                System.out.println("Sample: " + line.trim());
                break;
            }
        }
    }
    
    @Test
    public void testBase64ConversionPath() throws Exception {
        System.out.println("\n=== Testing Base64 Conversion Path (convertEmbeddedImagesToBase64 = true) ===");
        
        // Test with base64 conversion (AbstractOOXMLExtractor.handleEmbeddedFile should be used)
        ParseContext context = new ParseContext();
        OfficeParserConfig config = new OfficeParserConfig();
        config.setConvertEmbeddedImagesToBase64(true);  // Convert to base64 data URLs
        context.set(OfficeParserConfig.class, config);
        
        XMLResult result = getXML("testWORD_embeded.docx", context);
        String xml = result.xml;
        
        boolean hasEmbeddedUrls = xml.contains("src=\"embedded:");
        boolean hasDataUrls = xml.contains("src=\"data:");
        
        System.out.println("Has embedded: URLs: " + hasEmbeddedUrls);
        System.out.println("Has data: URLs: " + hasDataUrls);
        
        // Show sample img tags
        String[] lines = xml.split("\n");
        for (String line : lines) {
            if (line.contains("<img") && line.contains("alt=")) {
                System.out.println("Sample: " + line.trim());
                break;
            }
        }
    }
}
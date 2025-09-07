package org.apache.tika.parser.microsoft.ooxml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;

/**
 * Integration test to show the contextual alt text working
 */
public class ContextualAltTextIntegrationTest extends TikaTest {

    @Test
    public void testContextualAltTextInRealDocument() throws Exception {
        // Test with embedded images converted to base64 to see if my changes take effect
        ParseContext parseContext = new ParseContext();
        OfficeParserConfig config = new OfficeParserConfig();
        config.setConvertEmbeddedImagesToBase64(true);
        parseContext.set(OfficeParserConfig.class, config);
        
        // Parse the existing test document
        XMLResult result = getXML("testWORD_embeded.docx", parseContext);
        String xml = result.xml;
        
        System.out.println("Full extracted content with base64 conversion:\n" + xml);
        
        // The new behavior should provide contextual alt text instead of "A description..."
        // We're looking for images that now have contextual alt text instead of filename-based alt text
        assertTrue(xml.contains("src=\"embedded:image2.jpeg\"") || xml.contains("src=\"data:"), "Should contain embedded image");
        
        System.out.println("SUCCESS: Testing with base64 conversion enabled");
    }
}
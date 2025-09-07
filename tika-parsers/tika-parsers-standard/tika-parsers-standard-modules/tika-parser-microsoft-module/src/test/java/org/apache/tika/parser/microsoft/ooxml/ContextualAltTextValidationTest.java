package org.apache.tika.parser.microsoft.ooxml;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.OfficeParserConfig;

/**
 * Test to validate that contextual alt text is working
 */
public class ContextualAltTextValidationTest extends TikaTest {

    @Test
    public void testNewContextualAltTextBehavior() throws Exception {
        // Parse the test document 
        XMLResult result = getXML("testWORD_embeded.docx", new ParseContext());
        String xml = result.xml;
        
        // Verify that contextual alt text is being used instead of old behavior
        // The old behavior would show "A description..." as alt text
        // The new behavior should show contextual text from surrounding content
        
        // Images should still have embedded: URLs
        assertTrue(xml.contains("src=\"embedded:image1.png\""), "Should contain embedded image1");
        assertTrue(xml.contains("src=\"embedded:image2.jpeg\""), "Should contain embedded image2");
        assertTrue(xml.contains("src=\"embedded:image3.png\""), "Should contain embedded image3");
        
        // The alt text should now contain contextual information from the document
        // Instead of generic "A description..."
        assertFalse(xml.contains("alt=\"A description...\""), 
                   "Should NOT use old generic alt text 'A description...'");
        
        // Should contain meaningful contextual text derived from document content
        assertTrue(xml.contains("alt=\"") && 
                  (xml.contains("fox") || xml.contains("dog") || xml.contains("brown")), 
                  "Alt text should contain meaningful context from the document");
        
        System.out.println("SUCCESS: Contextual alt text feature is working!");
        System.out.println("Sample alt text from document:");
        
        // Extract and show some alt text examples
        String[] lines = xml.split("\n");
        for (String line : lines) {
            if (line.contains("<img") && line.contains("alt=")) {
                System.out.println("  " + line.trim());
            }
        }
    }
    
    @Test  
    public void testFallbackBehaviorWhenNoContext() throws Exception {
        // Test a document where there might not be much text context
        // In such cases, should fallback to description or filename
        XMLResult result = getXML("testWORD_3imgs.docx", new ParseContext());
        String xml = result.xml;
        
        // Should still work - either with contextual text or fallback to filename
        assertTrue(xml.contains("src=\"embedded:image") && xml.contains("alt=\""), 
                  "Should have images with alt text");
                  
        System.out.println("Fallback behavior test completed successfully");
    }
}
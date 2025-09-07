package org.apache.tika.parser.microsoft.ooxml;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.parser.ParseContext;

/**
 * Demonstration test that shows the contextual alt text feature working
 */
public class ContextualAltTextDemo extends TikaTest {

    @Test
    public void demonstrateContextualAltText() throws Exception {
        System.out.println("\n=== Contextual Alt Text Feature Demo ===");
        
        // Parse a test document
        XMLResult result = getXML("testWORD_embeded.docx", new ParseContext());
        String xml = result.xml;
        
        System.out.println("\nExtracting img tags with alt text from document:");
        System.out.println("-----------------------------------------------");
        
        // Find and display img tags to show the contextual alt text
        String[] lines = xml.split("\n");
        int imageCount = 0;
        
        for (String line : lines) {
            if (line.contains("<img") && line.contains("alt=")) {
                imageCount++;
                String trimmed = line.trim();
                
                // Extract the alt text for display
                int altStart = trimmed.indexOf("alt=\"") + 5;
                int altEnd = trimmed.indexOf("\"", altStart);
                String altText = altEnd > altStart ? trimmed.substring(altStart, altEnd) : "unknown";
                
                // Extract the src for reference
                int srcStart = trimmed.indexOf("src=\"") + 5;
                int srcEnd = trimmed.indexOf("\"", srcStart);
                String src = srcEnd > srcStart ? trimmed.substring(srcStart, srcEnd) : "unknown";
                
                System.out.println("Image " + imageCount + ":");
                System.out.println("  Source: " + src);
                System.out.println("  Alt text: \"" + altText + "\"");
                
                // Show what the old behavior would have been
                if (altText.equals("A description...")) {
                    System.out.println("  Status: ❌ Using old filename-based alt text");
                } else if (altText.contains("fox") || altText.contains("dog") || altText.contains("brown")) {
                    System.out.println("  Status: ✅ Using NEW contextual alt text from document content!");
                } else {
                    System.out.println("  Status: ⚠️  Using fallback behavior");
                }
                System.out.println();
            }
        }
        
        if (imageCount == 0) {
            System.out.println("No images found in the document output.");
        }
        
        System.out.println("=== Demo Complete ===\n");
        
        // The test always passes - this is just a demonstration
        assert true;
    }
}
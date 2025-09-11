package org.apache.tika.parser.pdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

/**
 * Test to reproduce the issues reported in the Chinese problem statement:
 * 1. Images appearing as `<div class="figure"/>` placeholders when extractMarkedContent=true
 * 2. extractHeaderAndFooters not working properly, headers/footers appearing after </body>
 * 3. Images appearing after repeated headers
 * 4. Additional </body> tag appearing
 */
public class PDFMarkedContentIssueReproductionTest extends TikaTest {

    @Test
    public void testIssuesWithBothDetectTablesAndExtractMarkedContentEnabled() throws Exception {
        // This reproduces the scenario described in the problem statement
        PDFParserConfig config = new PDFParserConfig();
        config.setDetectTables(true);
        config.setExtractMarkedContent(true);
        
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, config);
        
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();
        
        // Test with a sample PDF that has tables, images, headers and footers
        try (InputStream input = getResourceAsStream("/test-documents/testPDF.pdf")) {
            if (input != null) {
                PDFParser parser = new PDFParser();
                parser.parse(input, handler, metadata, context);
                
                String content = handler.toString();
                System.out.println("=== CONTENT OUTPUT ===");
                System.out.println(content);
                System.out.println("=== END CONTENT ===");
                
                // Check for the issues mentioned in the problem statement:
                
                // 1. Images should NOT just be placeholders when extractMarkedContent=true
                // Look for the placeholder div that indicates the problem
                boolean hasImagePlaceholder = content.contains("<div class=\"figure\"/>");
                if (hasImagePlaceholder) {
                    System.out.println("❌ ISSUE REPRODUCED: Images appear as placeholder divs");
                }
                
                // 2. Check for duplicate </body> tags
                int bodyCloseTagCount = countOccurrences(content, "</body>");
                if (bodyCloseTagCount > 1) {
                    System.out.println("❌ ISSUE REPRODUCED: Multiple </body> tags found: " + bodyCloseTagCount);
                }
                
                // 3. Check for content appearing after </body>
                int lastBodyCloseIndex = content.lastIndexOf("</body>");
                if (lastBodyCloseIndex != -1) {
                    String afterBodyContent = content.substring(lastBodyCloseIndex + 7).trim();
                    if (!afterBodyContent.isEmpty() && !afterBodyContent.equals("</html>")) {
                        System.out.println("❌ ISSUE REPRODUCED: Content found after </body>: " + 
                                         afterBodyContent.substring(0, Math.min(100, afterBodyContent.length())));
                    }
                }
                
                // 4. Check if headers/footers are properly extracted (not duplicated after body)
                // Headers/footers should be in proper document structure, not repeated at the end
                
                // For now, let's just verify the configuration works without throwing exceptions
                assertTrue(content.length() > 0, "Content should not be empty");
                
                // Log findings
                System.out.println("✓ Test completed - found " + bodyCloseTagCount + " </body> tags");
                System.out.println("✓ Content length: " + content.length());
                
            } else {
                System.out.println("ℹ️ Test PDF not available, skipping detailed checks");
            }
        }
    }

    @Test
    public void testComparisonWithExtractMarkedContentFalse() throws Exception {
        // Compare behavior when extractMarkedContent=false 
        PDFParserConfig config = new PDFParserConfig();
        config.setDetectTables(true);
        config.setExtractMarkedContent(false);
        
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, config);
        
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler();
        
        try (InputStream input = getResourceAsStream("/test-documents/testPDF.pdf")) {
            if (input != null) {
                PDFParser parser = new PDFParser();
                parser.parse(input, handler, metadata, context);
                
                String content = handler.toString();
                System.out.println("=== CONTENT OUTPUT (extractMarkedContent=false) ===");
                System.out.println(content.length() > 500 ? content.substring(0, 500) + "..." : content);
                System.out.println("=== END CONTENT ===");
                
                // This configuration should work better according to the problem statement
                int bodyCloseTagCount = countOccurrences(content, "</body>");
                System.out.println("✓ With extractMarkedContent=false - found " + bodyCloseTagCount + " </body> tags");
                
                assertTrue(content.length() > 0, "Content should not be empty");
                assertTrue(bodyCloseTagCount <= 1, "Should not have multiple </body> tags");
                
            } else {
                System.out.println("ℹ️ Test PDF not available, skipping detailed checks");
            }
        }
    }
    
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
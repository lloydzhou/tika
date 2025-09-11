package org.apache.tika.parser.pdf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;

/**
 * Test to check if the full HTML structure is being generated correctly
 * when using PDFMarkedContent2XHTML
 */
public class PDFMarkedContentHTMLStructureTest extends TikaTest {

    @Test
    public void testFullHTMLStructureWithExtractMarkedContent() throws Exception {
        PDFParserConfig config = new PDFParserConfig();
        config.setDetectTables(true);
        config.setExtractMarkedContent(true);
        
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, config);
        
        Metadata metadata = new Metadata();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ToHTMLContentHandler handler = new ToHTMLContentHandler(out, "UTF-8");
        
        // Test with a sample PDF
        try (InputStream input = getResourceAsStream("/test-documents/testPDF.pdf")) {
            if (input != null) {
                PDFParser parser = new PDFParser();
                parser.parse(input, handler, metadata, context);
                
                String htmlContent = out.toString("UTF-8");
                System.out.println("=== FULL HTML OUTPUT (extractMarkedContent=true) ===");
                System.out.println(htmlContent);
                System.out.println("=== END HTML ===");
                
                // Check for proper HTML structure
                assertTrue(htmlContent.contains("<html"), "Should contain opening html tag");
                assertTrue(htmlContent.contains("</html>"), "Should contain closing html tag");
                assertTrue(htmlContent.contains("<body"), "Should contain opening body tag");
                assertTrue(htmlContent.contains("</body>"), "Should contain closing body tag");
                
                // Count body tags
                int bodyOpenCount = countOccurrences(htmlContent, "<body");
                int bodyCloseCount = countOccurrences(htmlContent, "</body>");
                
                System.out.println("✓ Found " + bodyOpenCount + " <body> tags and " + bodyCloseCount + " </body> tags");
                
                // Should have exactly one of each
                assertTrue(bodyOpenCount == 1, "Should have exactly one <body> tag, found: " + bodyOpenCount);
                assertTrue(bodyCloseCount == 1, "Should have exactly one </body> tag, found: " + bodyCloseCount);
                
                // Check for content between body tags
                int bodyStart = htmlContent.indexOf("<body");
                int bodyEnd = htmlContent.indexOf("</body>");
                assertTrue(bodyStart != -1 && bodyEnd != -1 && bodyEnd > bodyStart, 
                          "Body tags should be properly positioned");
                
                String bodyContent = htmlContent.substring(bodyStart, bodyEnd);
                assertTrue(bodyContent.length() > 50, "Should have substantial content between body tags");
                
            } else {
                System.out.println("ℹ️ Test PDF not available, creating minimal test");
                String htmlContent = out.toString("UTF-8");
                // At minimum, we should have proper HTML structure even without content
                assertTrue(htmlContent.contains("<html") || htmlContent.length() > 0, "Should have HTML structure or content");
            }
        }
    }

    @Test 
    public void testCompareWithExtractMarkedContentFalse() throws Exception {
        PDFParserConfig config = new PDFParserConfig();
        config.setDetectTables(true);
        config.setExtractMarkedContent(false);
        
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, config);
        
        Metadata metadata = new Metadata();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ToHTMLContentHandler handler = new ToHTMLContentHandler(out, "UTF-8");
        
        try (InputStream input = getResourceAsStream("/test-documents/testPDF.pdf")) {
            if (input != null) {
                PDFParser parser = new PDFParser();
                parser.parse(input, handler, metadata, context);
                
                String htmlContent = out.toString("UTF-8");
                System.out.println("=== FULL HTML OUTPUT (extractMarkedContent=false) ===");
                System.out.println(htmlContent.length() > 1000 ? htmlContent.substring(0, 1000) + "..." : htmlContent);
                System.out.println("=== END HTML ===");
                
                // This should also have proper HTML structure
                assertTrue(htmlContent.contains("<html"), "Should contain opening html tag");
                assertTrue(htmlContent.contains("</html>"), "Should contain closing html tag");
                assertTrue(htmlContent.contains("<body"), "Should contain opening body tag");
                assertTrue(htmlContent.contains("</body>"), "Should contain closing body tag");
                
                // Count body tags
                int bodyOpenCount = countOccurrences(htmlContent, "<body");
                int bodyCloseCount = countOccurrences(htmlContent, "</body>");
                
                System.out.println("✓ Found " + bodyOpenCount + " <body> tags and " + bodyCloseCount + " </body> tags");
                
                // Should have exactly one of each
                assertTrue(bodyOpenCount == 1, "Should have exactly one <body> tag, found: " + bodyOpenCount);
                assertTrue(bodyCloseCount == 1, "Should have exactly one </body> tag, found: " + bodyCloseCount);
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
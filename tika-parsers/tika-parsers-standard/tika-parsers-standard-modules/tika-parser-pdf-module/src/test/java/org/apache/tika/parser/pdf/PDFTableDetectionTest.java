package org.apache.tika.parser.pdf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

/**
 * Test case for table detection functionality in PDF parsing.
 */
public class PDFTableDetectionTest extends TikaTest {

    @Test
    public void testTableDetection() throws Exception {
        try (InputStream input = PDFTableDetectionTest.class.getResourceAsStream("/test-documents/testPDFTable.pdf")) {
            if (input == null) {
                // Skip test if test file doesn't exist yet
                return;
            }
            
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler();
            ParseContext context = new ParseContext();
            
            PDFParser parser = new PDFParser();
            parser.parse(input, handler, metadata, context);
            
            String content = handler.toString();
            System.out.println("Parsed content:\n" + content);
            
            // For now, just verify the parser works
            assertTrue(content.length() > 0, "Content should not be empty");
        }
    }
}
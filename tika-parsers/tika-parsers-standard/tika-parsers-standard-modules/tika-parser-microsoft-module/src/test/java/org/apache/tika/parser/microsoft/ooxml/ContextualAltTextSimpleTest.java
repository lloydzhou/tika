package org.apache.tika.parser.microsoft.ooxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.sax.ContentHandlerDecorator;

/**
 * Unit test for contextual alt text functionality - simplified approach
 */
public class ContextualAltTextSimpleTest {

    /**
     * Mock handler to capture the alt text without full SAX processing
     */
    private static class MockXHTMLHandler extends ContentHandlerDecorator {
        private String capturedAlt;
        
        public void startElement(String localName, AttributesImpl attr) {
            if ("img".equals(localName) && attr != null) {
                capturedAlt = attr.getValue("alt");
            }
        }
        
        public String getCapturedAlt() {
            return capturedAlt;
        }
    }
    
    @Test
    public void testContextualAltTextGeneration() throws Exception {
        // Test the core functionality by creating a simple test extension
        OOXMLTikaBodyPartHandler handler = new OOXMLTikaBodyPartHandler(null);
        
        // Add some text to the buffer
        RunProperties runProps = new RunProperties();
        handler.run(runProps, "This is the first sentence.");
        handler.run(runProps, " This is the second sentence with context.");
        
        // Use reflection to access the private method for testing
        java.lang.reflect.Method method = OOXMLTikaBodyPartHandler.class.getDeclaredMethod("getContextualAltText");
        method.setAccessible(true);
        String contextualAlt = (String) method.invoke(handler);
        
        System.out.println("Generated contextual alt text: " + contextualAlt);
        
        // Verify the contextual alt text contains both sentences
        assertTrue(contextualAlt != null, "Should generate contextual alt text");
        assertTrue(contextualAlt.contains("first sentence"), "Should contain first sentence");
        assertTrue(contextualAlt.contains("second sentence"), "Should contain second sentence");
    }
    
    @Test
    public void testEmptyContextFallback() throws Exception {
        // Test fallback when no context is available
        OOXMLTikaBodyPartHandler handler = new OOXMLTikaBodyPartHandler(null);
        
        // Don't add any text - should return null
        java.lang.reflect.Method method = OOXMLTikaBodyPartHandler.class.getDeclaredMethod("getContextualAltText");
        method.setAccessible(true);
        String contextualAlt = (String) method.invoke(handler);
        
        assertEquals(null, contextualAlt, "Should return null when no context available");
    }
}
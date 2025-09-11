package org.apache.tika.parser.pdf;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.WriteOutContentHandler;

/**
 * Example demonstrating how to use Tika's PDF table detection feature.
 * This class shows how to configure and use the table detection functionality.
 */
public class TableDetectionExample {
    
    /**
     * Example configuration XML for enabling table detection
     */
    private static final String TIKA_CONFIG_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <properties>
          <parsers>
            <parser class="org.apache.tika.parser.pdf.PDFParser">
              <params>
                <param name="detectTables" type="bool">true</param>
                <param name="enableAutoSpace" type="bool">true</param>
                <param name="extractAnnotationText" type="bool">true</param>
              </params>
            </parser>
          </parsers>
        </properties>
        """;
    
    public static void main(String[] args) {
        demonstrateConfiguration();
        demonstrateTableDetectionSettings();
    }
    
    /**
     * Demonstrates how to configure Tika with XML configuration for table detection
     */
    public static void demonstrateConfiguration() {
        System.out.println("=== Table Detection Configuration Example ===");
        
        try {
            // Load configuration from XML
            TikaConfig config = new TikaConfig(
                new ByteArrayInputStream(TIKA_CONFIG_XML.getBytes(StandardCharsets.UTF_8))
            );
            
            Parser parser = config.getParser();
            System.out.println("✓ Successfully loaded Tika configuration with table detection enabled");
            System.out.println("✓ Parser class: " + parser.getClass().getSimpleName());
            
        } catch (Exception e) {
            System.err.println("✗ Failed to load configuration: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates how to programmatically configure table detection
     */
    public static void demonstrateTableDetectionSettings() {
        System.out.println("\n=== Programmatic Configuration Example ===");
        
        // Create PDF parser configuration
        PDFParserConfig config = new PDFParserConfig();
        
        // Show default settings
        System.out.println("Default table detection setting: " + config.isDetectTables());
        
        // Configure table detection
        config.setDetectTables(true);
        System.out.println("✓ Table detection enabled: " + config.isDetectTables());
        
        // Show other useful settings
        config.setEnableAutoSpace(true);
        config.setExtractAnnotationText(true);
        config.setSuppressDuplicateOverlappingText(false);
        
        System.out.println("✓ Auto space enabled: " + config.isEnableAutoSpace());
        System.out.println("✓ Annotation text extraction: " + config.isExtractAnnotationText());
        System.out.println("✓ Suppress duplicate text: " + config.isSuppressDuplicateOverlappingText());
        
        // Usage example
        System.out.println("\n=== Usage Example ===");
        System.out.println("To use this configuration:");
        System.out.println("1. ParseContext context = new ParseContext();");
        System.out.println("2. context.set(PDFParserConfig.class, config);");
        System.out.println("3. parser.parse(inputStream, handler, metadata, context);");
        System.out.println("\nTables will be extracted as HTML <table> elements with <tr> and <td> tags.");
    }
    
    /**
     * Returns example configuration content for reference
     */
    public static String getExampleConfigXML() {
        return TIKA_CONFIG_XML;
    }
}
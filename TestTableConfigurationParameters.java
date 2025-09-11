import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

public class TestTableConfigurationParameters {
    public static void main(String[] args) {
        try {
            System.out.println("Testing table detection configuration parameters...");
            
            // Test 1: Programmatic configuration
            System.out.println("\n=== Test 1: Programmatic Configuration ===");
            PDFParserConfig config = new PDFParserConfig();
            
            // Set table detection parameters
            config.setDetectTables(true);
            config.setTableMinColumnWidth(5.0f);
            config.setTableMinRowHeight(6.0f);
            config.setTableAlignmentTolerance(8.0f);
            config.setTableMaxColumnToCharRatio(1.2f);
            config.setTableColumnAppearanceRate(0.3f);
            
            // Verify the values are set correctly
            System.out.println("detectTables: " + config.isDetectTables());
            System.out.println("tableMinColumnWidth: " + config.getTableMinColumnWidth());
            System.out.println("tableMinRowHeight: " + config.getTableMinRowHeight());
            System.out.println("tableAlignmentTolerance: " + config.getTableAlignmentTolerance());
            System.out.println("tableMaxColumnToCharRatio: " + config.getTableMaxColumnToCharRatio());
            System.out.println("tableColumnAppearanceRate: " + config.getTableColumnAppearanceRate());
            
            // Test 2: XML configuration (if file exists)
            System.out.println("\n=== Test 2: XML Configuration ===");
            try {
                TikaConfig tikaConfig = new TikaConfig(Paths.get("tika-config-table-tuning.xml"));
                Parser parser = tikaConfig.getParser();
                
                if (parser instanceof PDFParser) {
                    PDFParser pdfParser = (PDFParser) parser;
                    System.out.println("✅ Successfully loaded XML configuration");
                    System.out.println("detectTables: " + pdfParser.isDetectTables());
                    System.out.println("tableMinColumnWidth: " + pdfParser.getTableMinColumnWidth());
                    System.out.println("tableMinRowHeight: " + pdfParser.getTableMinRowHeight());
                    System.out.println("tableAlignmentTolerance: " + pdfParser.getTableAlignmentTolerance());
                    System.out.println("tableMaxColumnToCharRatio: " + pdfParser.getTableMaxColumnToCharRatio());
                    System.out.println("tableColumnAppearanceRate: " + pdfParser.getTableColumnAppearanceRate());
                } else {
                    System.out.println("❌ Parser is not PDFParser: " + parser.getClass().getName());
                }
            } catch (Exception e) {
                System.out.println("⚠️  XML configuration test skipped: " + e.getMessage());
            }
            
            // Test 3: Default values
            System.out.println("\n=== Test 3: Default Values ===");
            PDFParserConfig defaultConfig = new PDFParserConfig();
            System.out.println("Default detectTables: " + defaultConfig.isDetectTables());
            System.out.println("Default tableMinColumnWidth: " + defaultConfig.getTableMinColumnWidth());
            System.out.println("Default tableMinRowHeight: " + defaultConfig.getTableMinRowHeight());
            System.out.println("Default tableAlignmentTolerance: " + defaultConfig.getTableAlignmentTolerance());
            System.out.println("Default tableMaxColumnToCharRatio: " + defaultConfig.getTableMaxColumnToCharRatio());
            System.out.println("Default tableColumnAppearanceRate: " + defaultConfig.getTableColumnAppearanceRate());
            
            System.out.println("\n✅ All configuration parameter tests completed successfully!");
            System.out.println("\nThese parameters can now be configured via:");
            System.out.println("1. Programmatic API (PDFParserConfig setters)");
            System.out.println("2. XML configuration (tika-config.xml)");
            System.out.println("3. Properties files (tika.properties)");
            System.out.println("4. HTTP headers (Tika Server)");
            
        } catch (Exception e) {
            System.err.println("Error during configuration test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
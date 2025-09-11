# PDF Table Detection in Apache Tika

This document explains how to use Apache Tika's PDF table detection feature, which automatically identifies and extracts tabular data from PDF documents.

## Overview

The PDF table detection feature uses PDFBox's text positioning information to identify tabular structures in PDF documents. It analyzes the spatial positioning of text elements to detect rows and columns, then outputs the detected tables as HTML `<table>` elements.

**Note**: As of version 4.0.0, table detection works correctly with both regular PDF parsing (`extractMarkedContent=false`) and marked content extraction (`extractMarkedContent=true`). Previous versions had an issue where table detection only worked when `extractMarkedContent=true` was set.

## Configuration

### Method 1: XML Configuration (tika-config.xml)

Create a `tika-config.xml` file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<properties>
  <parsers>
    <parser class="org.apache.tika.parser.pdf.PDFParser">
      <params>
        <!-- Enable table detection (default: true) -->
        <param name="detectTables" type="bool">true</param>
        
        <!-- Table detection algorithm parameters -->
        <param name="tableMinColumnWidth" type="float">5.0</param>
        <param name="tableMinRowHeight" type="float">6.0</param>
        <param name="tableAlignmentTolerance" type="float">8.0</param>
        <param name="tableMaxColumnToCharRatio" type="float">1.2</param>
        <param name="tableColumnAppearanceRate" type="float">0.3</param>
        
        <!-- Optional: Enable marked content extraction (default: false) -->
        <param name="extractMarkedContent" type="bool">false</param>
        
        <!-- Other useful PDF parser options -->
        <param name="extractInlineImages" type="bool">false</param>
        <param name="extractUniqueInlineImagesOnly" type="bool">true</param>
        <param name="sortByPosition" type="bool">false</param>
        <param name="enableAutoSpace" type="bool">true</param>
        <param name="suppressDuplicateOverlappingText" type="bool">false</param>
        <param name="extractAnnotationText" type="bool">true</param>
        <param name="extractAcroFormContent" type="bool">true</param>
        <param name="extractBookmarksText" type="bool">true</param>
      </params>
    </parser>
  </parsers>
</properties>
```

Load the configuration:

```java
TikaConfig config = new TikaConfig(Paths.get("tika-config.xml"));
Parser parser = config.getParser();
```

### Method 2: Properties Configuration (tika.properties)

Create a `tika.properties` file:

```properties
# Enable table detection in PDF parser (default is true)
pdf.detectTables=true

# Table detection algorithm parameters
pdf.tableMinColumnWidth=5.0
pdf.tableMinRowHeight=6.0
pdf.tableAlignmentTolerance=8.0
pdf.tableMaxColumnToCharRatio=1.2
pdf.tableColumnAppearanceRate=0.3

# Optional: Enable marked content extraction (default is false)
pdf.extractMarkedContent=false

# Other useful PDF parser options  
pdf.extractInlineImages=false
pdf.enableAutoSpace=true
pdf.extractAnnotationText=true
```

### Method 3: Programmatic Configuration

```java
// Create PDF parser configuration
PDFParserConfig config = new PDFParserConfig();

// Enable table detection (enabled by default)
config.setDetectTables(true);

// Configure table detection algorithm parameters
config.setTableMinColumnWidth(5.0f);
config.setTableMinRowHeight(6.0f);
config.setTableAlignmentTolerance(8.0f);
config.setTableMaxColumnToCharRatio(1.2f);
config.setTableColumnAppearanceRate(0.3f);

// Optional: Enable marked content extraction
config.setExtractMarkedContent(false);

// Configure other useful options
config.setEnableAutoSpace(true);
config.setExtractAnnotationText(true);

// Use the configuration
ParseContext context = new ParseContext();
context.set(PDFParserConfig.class, config);

// Parse the PDF
PDFParser parser = new PDFParser();
parser.parse(inputStream, handler, metadata, context);
```

## Table Detection with Marked Content

Table detection now works correctly with both parsing modes:

### Regular Parsing (extractMarkedContent=false)
- Uses standard PDF text extraction
- Table detection works through text position analysis
- Suitable for most PDF documents
- Better performance for documents without marked content structure

### Marked Content Parsing (extractMarkedContent=true)  
- Extracts text using PDF marked content structure tags
- Table detection works alongside marked content extraction
- Better for PDFs with proper accessibility markup
- Preserves structural information from the PDF

Both modes can be used simultaneously with image extraction without conflicts.

## Usage Example

```java
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;

// Configure table detection
PDFParserConfig config = new PDFParserConfig();
config.setDetectTables(true);

// Set up parsing components
Metadata metadata = new Metadata();
BodyContentHandler handler = new BodyContentHandler();
ParseContext context = new ParseContext();
context.set(PDFParserConfig.class, config);

// Parse the PDF with table detection
PDFParser parser = new PDFParser();
try (InputStream input = new FileInputStream("document.pdf")) {
    parser.parse(input, handler, metadata, context);
    
    // Get the extracted content with HTML tables
    String content = handler.toString();
    System.out.println(content);
}
```

## Output Format

Detected tables are output as HTML table elements:

```html
<table>
  <tr>
    <td>Name</td>
    <td>Age</td>
    <td>Country</td>
  </tr>
  <tr>
    <td>John</td>
    <td>25</td>
    <td>USA</td>
  </tr>
  <tr>
    <td>Mary</td>
    <td>30</td>
    <td>UK</td>
  </tr>
</table>
```

## Algorithm Parameters

The table detection algorithm uses several configurable parameters that can be adjusted for better detection:

### Configurable Parameters

- **tableMinColumnWidth**: Minimum column width in pixels (default: 5.0)
  - Columns closer together than this value may not be detected as separate columns
  - Reduce for narrow columns, increase to avoid false column detection

- **tableMinRowHeight**: Minimum row height in pixels (default: 6.0)
  - Rows closer together than this value may not be detected as separate rows
  - Reduce for tightly spaced tables, increase for widely spaced content

- **tableAlignmentTolerance**: Alignment tolerance in pixels (default: 8.0)
  - Text positions within this tolerance are considered aligned in the same column
  - Increase for more flexible alignment detection, decrease for stricter alignment

- **tableMaxColumnToCharRatio**: Maximum column-to-character ratio (default: 1.2)
  - Tables with higher ratios may be rejected as false positives
  - Increase to allow more columns relative to content, decrease to be more strict

- **tableColumnAppearanceRate**: Minimum column appearance rate (default: 0.3 or 30%)
  - Columns must appear in at least this percentage of rows to be considered valid
  - Reduce for more lenient detection, increase for stricter column consistency

### Fixed Parameters

- **Minimum Columns**: 2 (at least 2 columns required for table detection)
- **Minimum Rows**: 2 (at least 2 rows required for table detection)

## Configuration Tuning

### For Better Detection of Narrow Tables
If your tables have very narrow columns or Chinese/Asian characters:
```xml
<param name="tableMinColumnWidth" type="float">3.0</param>
<param name="tableAlignmentTolerance" type="float">10.0</param>
```

### For Strict Table Detection
If you're getting too many false positives:
```xml
<param name="tableMaxColumnToCharRatio" type="float">0.8</param>
<param name="tableColumnAppearanceRate" type="float">0.5</param>
<param name="tableAlignmentTolerance" type="float">5.0</param>
```

### For Loose/Irregular Tables
If your tables have irregular spacing or alignment:
```xml
<param name="tableAlignmentTolerance" type="float">12.0</param>
<param name="tableColumnAppearanceRate" type="float">0.2</param>
<param name="tableMaxColumnToCharRatio" type="float">1.5</param>
```

### For Dense Content with Many Small Tables
```xml
<param name="tableMinColumnWidth" type="float">8.0</param>
<param name="tableMinRowHeight" type="float">10.0</param>
<param name="tableColumnAppearanceRate" type="float">0.4</param>
```

## Best Practices

1. **Enable Auto Space**: Set `enableAutoSpace=true` for better text extraction
2. **Handle Empty Cells**: The algorithm handles sparse data where some cells may be empty
3. **Multiple Tables**: The algorithm can detect multiple tables on the same page
4. **Performance**: Table detection has minimal performance impact on PDF parsing
5. **Test Parameters**: Start with default values and adjust based on your specific PDF content
6. **Monitor False Positives**: If getting too many false tables, increase `tableMaxColumnToCharRatio` threshold

## Troubleshooting

### Tables Not Detected

If tables aren't being detected:

1. Check that `detectTables=true` is set in your configuration
2. Verify the table has regular column alignment (within your `tableAlignmentTolerance` setting)
3. Ensure columns are at least `tableMinColumnWidth` pixels apart
4. Check that the table has at least 2 rows and 2 columns
5. Verify that columns appear consistently across at least `tableColumnAppearanceRate` percentage of rows
6. Try reducing `tableMinColumnWidth` for narrow columns
7. Try increasing `tableAlignmentTolerance` for irregular alignment
8. Try reducing `tableColumnAppearanceRate` for sparse tables

### Debug Information

To debug table detection issues, you can:

1. Enable font name extraction: `extractFontNames=true`
2. Enable marked content extraction: `extractMarkedContent=true`
3. Check the raw text positions in the PDF

### Common Issues

- **Scanned PDFs**: Table detection only works on PDFs with selectable text, not scanned images
- **Complex Layouts**: Tables with merged cells or irregular layouts may not be detected
- **Small Tables**: Very small tables (less than 2x2) are not detected
- **Rotated Tables**: Tables that are rotated may not be detected correctly

## Integration with Tika Server

When using Tika Server, you can pass configuration through HTTP headers or by providing a tika-config.xml file to the server.

Example curl command:
```bash
curl -X POST \
  -H "Content-Type: application/pdf" \
  -H "X-Tika-PDFdetectTables: true" \
  --data-binary @document.pdf \
  http://localhost:9998/tika
```

## Version Compatibility

This table detection feature is available in Apache Tika 4.0.0 and later versions.
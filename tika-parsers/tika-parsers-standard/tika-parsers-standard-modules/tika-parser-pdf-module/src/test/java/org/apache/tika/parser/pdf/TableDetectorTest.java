package org.apache.tika.parser.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

/**
 * Unit test for TableDetector functionality.
 */
public class TableDetectorTest {

    @Test
    public void testBasicTableDetection() {
        List<TextPosition> textPositions = createSimpleTableLayout();
        
        List<TableDetector.TableStructure> tables = TableDetector.detectTables(textPositions);
        
        assertFalse(tables.isEmpty(), "Should detect at least one table");
        
        TableDetector.TableStructure table = tables.get(0);
        assertTrue(table.getRows().size() >= 2, "Should have at least 2 rows");
        assertTrue(table.getRows().get(0).getCells().size() >= 2, "Should have at least 2 columns");
    }
    
    @Test
    public void testNoTableDetection() {
        List<TextPosition> textPositions = createLinearLayout();
        
        List<TableDetector.TableStructure> tables = TableDetector.detectTables(textPositions);
        
        assertTrue(tables.isEmpty(), "Should not detect tables in linear layout");
    }
    
    @Test
    public void testEmptyInput() {
        List<TextPosition> textPositions = new ArrayList<>();
        
        List<TableDetector.TableStructure> tables = TableDetector.detectTables(textPositions);
        
        assertTrue(tables.isEmpty(), "Should not detect tables in empty input");
    }

    private List<TextPosition> createSimpleTableLayout() {
        List<TextPosition> positions = new ArrayList<>();
        
        // Create a simple 2x2 table layout
        // Row 1: "Name" at (10, 100), "Age" at (100, 100)
        // Row 2: "John" at (10, 80), "25" at (100, 80)
        
        positions.add(createMockTextPosition("Name", 10, 100, 40, 10));
        positions.add(createMockTextPosition("Age", 100, 100, 30, 10));
        positions.add(createMockTextPosition("John", 10, 80, 40, 10));
        positions.add(createMockTextPosition("25", 100, 80, 20, 10));
        
        return positions;
    }
    
    private List<TextPosition> createLinearLayout() {
        List<TextPosition> positions = new ArrayList<>();
        
        // Create a linear layout that should not be detected as a table
        positions.add(createMockTextPosition("This", 10, 100, 30, 10));
        positions.add(createMockTextPosition("is", 50, 100, 20, 10));
        positions.add(createMockTextPosition("a", 80, 100, 10, 10));
        positions.add(createMockTextPosition("sentence", 100, 100, 60, 10));
        
        return positions;
    }
    
    private TextPosition createMockTextPosition(String text, float x, float y, float width, float height) {
        TextPosition mockPos = mock(TextPosition.class);
        when(mockPos.getUnicode()).thenReturn(text);
        when(mockPos.getX()).thenReturn(x);
        when(mockPos.getY()).thenReturn(y);
        when(mockPos.getWidth()).thenReturn(width);
        when(mockPos.getHeight()).thenReturn(height);
        return mockPos;
    }
}
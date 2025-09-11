/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.pdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.text.TextPosition;

/**
 * Utility class for detecting table structures in PDF content based on
 * the spatial positioning of text elements.
 */
class TableDetector {
    
    private static final float MIN_COLUMN_WIDTH = 5f;  // Further reduced for narrow columns and Chinese characters
    private static final float MIN_ROW_HEIGHT = 6f;   // Reduced for tighter row spacing
    private static final int MIN_ROWS = 2;
    private static final int MIN_COLUMNS = 2;
    private static final float ALIGNMENT_TOLERANCE = 8f; // Increased for more flexible alignment detection
    private static final float MAX_COLUMN_TO_CHAR_RATIO = 1.2f; // Increased to allow more columns relative to content
    
    /**
     * Represents a detected table structure.
     */
    static class TableStructure {
        private final List<TableRow> rows;
        private final float x, y, width, height;
        
        TableStructure(List<TableRow> rows, float x, float y, float width, float height) {
            this.rows = rows;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        List<TableRow> getRows() { return rows; }
        float getX() { return x; }
        float getY() { return y; }
        float getWidth() { return width; }
        float getHeight() { return height; }
    }
    
    /**
     * Represents a table row.
     */
    static class TableRow {
        private final List<TableCell> cells;
        private final float y;
        
        TableRow(List<TableCell> cells, float y) {
            this.cells = cells;
            this.y = y;
        }
        
        List<TableCell> getCells() { return cells; }
        float getY() { return y; }
    }
    
    /**
     * Represents a table cell.
     */
    static class TableCell {
        private final String text;
        private final float x, y, width, height;
        
        TableCell(String text, float x, float y, float width, float height) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        String getText() { return text; }
        float getX() { return x; }
        float getY() { return y; }
        float getWidth() { return width; }
        float getHeight() { return height; }
    }
    
    /**
     * Detects table structures in the given list of text positions.
     */
    static List<TableStructure> detectTables(List<TextPosition> textPositions) {
        return detectTables(textPositions, new PDFParserConfig());
    }
    
    /**
     * Detects table structures in the given list of text positions using the provided configuration.
     */
    static List<TableStructure> detectTables(List<TextPosition> textPositions, PDFParserConfig config) {
        if (textPositions.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Group text positions by approximate Y coordinate (rows)
        Map<Float, List<TextPosition>> rowGroups = groupByRows(textPositions, config);
        
        if (rowGroups.size() < MIN_ROWS) {
            return Collections.emptyList();
        }
        
        // Analyze column alignment across rows
        List<Float> columnPositions = detectColumnPositions(rowGroups, config);
        
        if (columnPositions.size() < MIN_COLUMNS) {
            return Collections.emptyList();
        }
        
        // Build table structure
        List<TableStructure> tables = new ArrayList<>();
        TableStructure table = buildTableStructure(rowGroups, columnPositions, config);
        if (table != null) {
            tables.add(table);
        }
        
        return tables;
    }
    
    private static Map<Float, List<TextPosition>> groupByRows(List<TextPosition> textPositions, PDFParserConfig config) {
        Map<Float, List<TextPosition>> rowGroups = new HashMap<>();
        
        for (TextPosition pos : textPositions) {
            float y = pos.getY();
            
            // Skip positions with null or empty text
            if (pos.getUnicode() == null || pos.getUnicode().trim().isEmpty()) {
                continue;
            }
            
            // Find existing row with similar Y coordinate
            Float matchingY = null;
            for (Float existingY : rowGroups.keySet()) {
                if (Math.abs(y - existingY) <= config.getTableAlignmentTolerance()) {
                    matchingY = existingY;
                    break;
                }
            }
            
            if (matchingY == null) {
                matchingY = y;
                rowGroups.put(matchingY, new ArrayList<>());
            }
            
            rowGroups.get(matchingY).add(pos);
        }
        
        return rowGroups;
    }
    
    private static List<Float> detectColumnPositions(Map<Float, List<TextPosition>> rowGroups, PDFParserConfig config) {
        Map<Float, Integer> columnCounts = new HashMap<>();
        
        // Count how often each X position appears across rows
        for (List<TextPosition> row : rowGroups.values()) {
            for (TextPosition pos : row) {
                float x = pos.getX();
                
                // Skip positions with null or empty text
                if (pos.getUnicode() == null || pos.getUnicode().trim().isEmpty()) {
                    continue;
                }
                
                // Find existing column with similar X coordinate
                Float matchingX = null;
                for (Float existingX : columnCounts.keySet()) {
                    if (Math.abs(x - existingX) <= config.getTableAlignmentTolerance()) {
                        matchingX = existingX;
                        break;
                    }
                }
                
                if (matchingX == null) {
                    matchingX = x;
                }
                
                columnCounts.put(matchingX, columnCounts.getOrDefault(matchingX, 0) + 1);
            }
        }
        
        // Filter columns that appear in multiple rows (table-like alignment)
        int minAppearances = Math.max(MIN_ROWS, (int)(rowGroups.size() * config.getTableColumnAppearanceRate()));
        List<Float> columnPositions = new ArrayList<>();
        
        for (Map.Entry<Float, Integer> entry : columnCounts.entrySet()) {
            if (entry.getValue() >= minAppearances) {
                columnPositions.add(entry.getKey());
            }
        }
        
        Collections.sort(columnPositions);
        
        // Additional check: ensure columns are reasonably spaced
        if (columnPositions.size() >= MIN_COLUMNS) {
            for (int i = 1; i < columnPositions.size(); i++) {
                float spacing = columnPositions.get(i) - columnPositions.get(i - 1);
                if (spacing < config.getTableMinColumnWidth()) {
                    // Columns too close together, likely not a table
                    return new ArrayList<>();
                }
            }
        }
        
        return columnPositions;
    }
    
    private static TableStructure buildTableStructure(Map<Float, List<TextPosition>> rowGroups, List<Float> columnPositions, PDFParserConfig config) {
        List<TableRow> rows = new ArrayList<>();
        
        // Sort rows by Y position (descending, PDF coordinates are bottom-up)
        List<Float> sortedYPositions = new ArrayList<>(rowGroups.keySet());
        Collections.sort(sortedYPositions, Collections.reverseOrder());
        
        float minX = Collections.min(columnPositions);
        float maxX = Collections.max(columnPositions);
        float minY = Collections.min(sortedYPositions);
        float maxY = Collections.max(sortedYPositions);
        
        for (Float y : sortedYPositions) {
            List<TextPosition> rowPositions = rowGroups.get(y);
            
            // Sort positions in this row by X coordinate
            Collections.sort(rowPositions, (a, b) -> Float.compare(a.getX(), b.getX()));
            
            List<TableCell> cells = new ArrayList<>();
            
            // Create cells based on column positions
            for (int i = 0; i < columnPositions.size(); i++) {
                float colX = columnPositions.get(i);
                
                // Find text position closest to this column
                TextPosition closestPos = null;
                float minDistance = Float.MAX_VALUE;
                
                for (TextPosition pos : rowPositions) {
                    float distance = Math.abs(pos.getX() - colX);
                    // Use configurable tolerance for matching text to columns
                    if (distance < minDistance && distance <= config.getTableAlignmentTolerance() * 4) {
                        minDistance = distance;
                        closestPos = pos;
                    }
                }
                
                if (closestPos != null) {
                    String text = closestPos.getUnicode();
                    float cellWidth = (i < columnPositions.size() - 1) 
                        ? columnPositions.get(i + 1) - colX 
                        : maxX - colX + MIN_COLUMN_WIDTH;
                    
                    TableCell cell = new TableCell(text, colX, y, cellWidth, closestPos.getHeight());
                    cells.add(cell);
                    rowPositions.remove(closestPos);
                } else {
                    // Empty cell
                    float cellWidth = (i < columnPositions.size() - 1) 
                        ? columnPositions.get(i + 1) - colX 
                        : config.getTableMinColumnWidth();
                    TableCell cell = new TableCell("", colX, y, cellWidth, config.getTableMinRowHeight());
                    cells.add(cell);
                }
            }
            
            if (!cells.isEmpty()) {
                rows.add(new TableRow(cells, y));
            }
        }
        
        if (rows.size() >= MIN_ROWS && !columnPositions.isEmpty()) {
            // Additional validation: check if we have a proper grid structure
            boolean hasValidGrid = true;
            int expectedColumns = columnPositions.size();
            
            for (TableRow row : rows) {
                // Allow more flexibility in number of cells per row - reduced from 1/2 to 1/3
                if (row.getCells().size() < Math.max(1, expectedColumns / 3)) {
                    hasValidGrid = false;
                    break;
                }
            }
            
            if (hasValidGrid) {
                float tableWidth = maxX - minX + config.getTableMinColumnWidth();
                float tableHeight = maxY - minY + config.getTableMinRowHeight();
                TableStructure table = new TableStructure(rows, minX, minY, tableWidth, tableHeight);
                
                // Additional validation to prevent false positives (header/footer detection)
                if (isLikelyTable(table, config)) {
                    return table;
                } else {
                    return null; // Reject this as a false positive
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validate that a detected table structure is likely a real table and not 
     * a false positive (such as header/footer text laid out in a grid pattern).
     */
    private static boolean isLikelyTable(TableStructure table, PDFParserConfig config) {
        if (table.getRows().isEmpty()) {
            return false;
        }
        
        // Calculate column-to-character ratio to detect false positives
        int totalCells = 0;
        int totalCharacters = 0;
        int nonEmptyColumns = 0;
        
        for (TableRow row : table.getRows()) {
            totalCells += row.getCells().size();
            
            for (TableCell cell : row.getCells()) {
                String cellText = cell.getText();
                if (cellText != null && !cellText.trim().isEmpty()) {
                    totalCharacters += cellText.length();
                    nonEmptyColumns++;
                }
            }
        }
        
        // If we have very few characters, don't apply the ratio check strictly
        if (totalCharacters < 15) {
            return true;
        }
        
        // Calculate the ratio of total cells to total characters
        float columnToCharRatio = (float) totalCells / Math.max(1, totalCharacters);
        
        // If the ratio is too high (many columns relative to content), 
        // it's likely a false positive (header/footer with character-per-column layout)
        if (columnToCharRatio > config.getTableMaxColumnToCharRatio()) {
            return false;
        }
        
        // Additional check: if average characters per cell is too low, likely false positive
        // Made more lenient for simple tables with short content
        float avgCharsPerCell = (float) totalCharacters / Math.max(1, nonEmptyColumns);
        
        if (avgCharsPerCell < 1.0f && columnToCharRatio > 0.8f) {
            return false;
        }
        
        return true;
    }
}
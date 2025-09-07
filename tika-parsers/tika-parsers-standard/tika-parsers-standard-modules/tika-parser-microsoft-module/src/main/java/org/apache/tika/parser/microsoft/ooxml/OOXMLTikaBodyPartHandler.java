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

package org.apache.tika.parser.microsoft.ooxml;


import java.math.BigInteger;
import java.util.Date;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.microsoft.WordExtractor;
import org.apache.tika.parser.microsoft.ooxml.xwpf.XWPFStylesShim;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.parser.ParseContext;

public class OOXMLTikaBodyPartHandler
        implements OOXMLWordAndPowerPointTextHandler.XWPFBodyContentsHandler {

    private static final String P = "p";

    private static final char[] NEWLINE = new char[]{'\n'};
    
    // Buffer size for text context tracking - keep last 2000 characters
    private static final int TEXT_BUFFER_SIZE = 2000;

    private final XHTMLContentHandler xhtml;
    private final XWPFListManager listManager;
    private final boolean includeDeletedText;
    private final boolean includeMoveFromText;
    private final XWPFStylesShim styles;
    private final ParseContext context;
    
    // Buffer to track recent text for context
    private final StringBuilder textBuffer = new StringBuilder(TEXT_BUFFER_SIZE);

    private int pDepth = 0; //paragraph depth
    private int tableDepth = 0;//table depth
    private int sdtDepth = 0;//
    private boolean isItalics = false;
    private boolean isBold = false;
    private boolean isUnderline = false;
    private boolean isStrikeThrough = false;
    private boolean wroteHyperlinkStart = false;

    //TODO: fix this
    //pWithinCell should be an array/stack of given cell depths
    //so that when you get to the end of an embedded table, e.g.,
    //you know what your paragraph count was in the parent cell.
    //<tc><p/><p/><table><tr><tc></p></p></tc></tr></table>...
    private int tableCellDepth = 0;
    private int pWithinCell = 0;

    //will need to replace this with a stack
    //if we're marking more that the first level <p/> element
    private String paragraphTag = null;

    public OOXMLTikaBodyPartHandler(XHTMLContentHandler xhtml) {
        this(xhtml, (ParseContext) null);
    }

    public OOXMLTikaBodyPartHandler(XHTMLContentHandler xhtml, ParseContext context) {
        this.xhtml = xhtml;
        this.styles = XWPFStylesShim.EMPTY_STYLES;
        this.listManager = XWPFListManager.EMPTY_LIST;
        this.includeDeletedText = false;
        this.includeMoveFromText = false;
        this.context = context;
    }

    public OOXMLTikaBodyPartHandler(XHTMLContentHandler xhtml, XWPFStylesShim styles,
                                    XWPFListManager listManager, OfficeParserConfig parserConfig) {
        this(xhtml, styles, listManager, parserConfig, null);
    }

    public OOXMLTikaBodyPartHandler(XHTMLContentHandler xhtml, XWPFStylesShim styles,
                                    XWPFListManager listManager, OfficeParserConfig parserConfig, ParseContext context) {
        this.xhtml = xhtml;
        this.styles = styles;
        this.listManager = listManager;
        this.includeDeletedText = parserConfig.isIncludeDeletedContent();
        this.includeMoveFromText = parserConfig.isIncludeMoveFromContent();
        this.context = context;
    }

    @Override
    public void run(RunProperties runProperties, String contents) throws SAXException {

        // Add text to our buffer for context tracking first
        addToTextBuffer(contents);
        
        // Skip XHTML processing if handler is null (for testing)
        if (xhtml == null) {
            return;
        }

        // True if we are currently in the named style tag:
        if (runProperties.isBold() != isBold) {
            if (isStrikeThrough) {
                xhtml.endElement("strike");
                isStrikeThrough = false;
            }
            if (isUnderline) {
                xhtml.endElement("u");
                isUnderline = false;
            }
            if (isItalics) {
                xhtml.endElement("i");
                isItalics = false;
            }
            if (runProperties.isBold()) {
                xhtml.startElement("b");
            } else {
                xhtml.endElement("b");
            }
            isBold = runProperties.isBold();
        }

        if (runProperties.isItalics() != isItalics) {
            if (isStrikeThrough) {
                xhtml.endElement("strike");
                isStrikeThrough = false;
            }
            if (isUnderline) {
                xhtml.endElement("u");
                isUnderline = false;
            }
            if (runProperties.isItalics()) {
                xhtml.startElement("i");
            } else {
                xhtml.endElement("i");
            }
            isItalics = runProperties.isItalics();
        }

        if (runProperties.isStrikeThrough() != isStrikeThrough) {
            if (isUnderline) {
                xhtml.endElement("u");
                isUnderline = false;
            }
            if (runProperties.isStrikeThrough()) {
                xhtml.startElement("strike");
            } else {
                xhtml.endElement("strike");
            }
            isStrikeThrough = runProperties.isStrikeThrough();
        }

        boolean runIsUnderlined = runProperties.getUnderline() != UnderlinePatterns.NONE;
        if (runIsUnderlined != isUnderline) {
            if (runIsUnderlined) {
                xhtml.startElement("u");
            } else {
                xhtml.endElement("u");
            }
            isUnderline = runIsUnderlined;
        }

        xhtml.characters(contents);

    }

    @Override
    public void hyperlinkStart(String link) throws SAXException {
        if (link != null) {
            xhtml.startElement("a", "href", link);
            wroteHyperlinkStart = true;
        }
    }

    @Override
    public void hyperlinkEnd() throws SAXException {
        if (wroteHyperlinkStart) {
            closeStyleTags();
            wroteHyperlinkStart = false;
            xhtml.endElement("a");
        }
    }

    @Override
    public void startParagraph(ParagraphProperties paragraphProperties) throws SAXException {

        //if you're in a table cell and your after the first paragraph
        //make sure to prepend a \n
        if (tableCellDepth > 0 && pWithinCell > 0) {
            xhtml.characters(NEWLINE, 0, 1);
        }

        if (pDepth == 0 && tableDepth == 0 && sdtDepth == 0) {
            paragraphTag = P;
            String styleClass = null;
            //TIKA-2144 check that styles is not null
            if (paragraphProperties.getStyleID() != null && styles != null) {
                String styleName = styles.getStyleName(paragraphProperties.getStyleID());
                if (styleName != null) {
                    WordExtractor.TagAndStyle tas =
                            WordExtractor.buildParagraphTagAndStyle(styleName, false);
                    paragraphTag = tas.getTag();
                    styleClass = tas.getStyleClass();
                }
            }


            if (styleClass == null) {
                xhtml.startElement(paragraphTag);
            } else {
                xhtml.startElement(paragraphTag, "class", styleClass);
            }
        }

        writeParagraphNumber(paragraphProperties.getNumId(), paragraphProperties.getIlvl(),
                listManager, xhtml);
        pDepth++;
    }


    @Override
    public void endParagraph() throws SAXException {
        closeStyleTags();
        if (pDepth == 1 && tableDepth == 0) {
            if (xhtml != null) {
                xhtml.endElement(paragraphTag);
            }
        } else if (tableCellDepth > 0 && pWithinCell > 0) {
            if (xhtml != null) {
                xhtml.characters(NEWLINE, 0, 1);
            }
        } else if (tableCellDepth == 0) {
            if (xhtml != null) {
                xhtml.characters(NEWLINE, 0, 1);
            }
        }

        if (tableCellDepth > 0) {
            pWithinCell++;
        }
        pDepth--;
        
        // Add paragraph break to text buffer to help with sentence context
        addToTextBuffer(" ");
    }

    @Override
    public void startTable() throws SAXException {

        xhtml.startElement("table");
        tableDepth++;

    }

    @Override
    public void endTable() throws SAXException {

        xhtml.endElement("table");
        tableDepth--;

    }

    @Override
    public void startTableRow() throws SAXException {
        xhtml.startElement("tr");
    }

    @Override
    public void endTableRow() throws SAXException {
        xhtml.endElement("tr");
    }

    @Override
    public void startTableCell() throws SAXException {
        xhtml.startElement("td");
        tableCellDepth++;
    }

    @Override
    public void endTableCell() throws SAXException {
        xhtml.endElement("td");
        pWithinCell = 0;
        tableCellDepth--;
    }

    @Override
    public void startSDT() throws SAXException {
        closeStyleTags();
        sdtDepth++;
    }

    @Override
    public void endSDT() {
        sdtDepth--;
    }

    @Override
    public void startEditedSection(String editor, Date date,
                                   OOXMLWordAndPowerPointTextHandler.EditType editType) {
        //no-op
    }

    @Override
    public void endEditedSection() {
        //no-op
    }

    @Override
    public boolean isIncludeDeletedText() {
        return includeDeletedText;
    }

    @Override
    public void footnoteReference(String id) throws SAXException {
        if (id != null) {
            xhtml.characters("[");
            xhtml.characters(id);
            xhtml.characters("]");
        }
    }

    @Override
    public void endnoteReference(String id) throws SAXException {
        if (id != null) {
            xhtml.characters("[");
            xhtml.characters(id);
            xhtml.characters("]");
        }
    }

    @Override
    public boolean isIncludeMoveFromText() {
        return includeMoveFromText;
    }

    @Override
    public void embeddedOLERef(String relId) throws SAXException {
        if (relId == null) {
            return;
        }
        AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute("", "class", "class", "CDATA", "embedded");
        attributes.addAttribute("", "id", "id", "CDATA", relId);
        xhtml.startElement("div", attributes);
        xhtml.endElement("div");
    }

    @Override
    public void embeddedPicRef(String picFileName, String picDescription) throws SAXException {
        // Since this method doesn't have access to image data, we can only create embedded: URLs
        // The actual base64 conversion happens in AbstractOOXMLExtractor.handleEmbeddedFile()
        // when the image data is available.
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute("", "src", "src", "CDATA", "embedded:" + picFileName);
        
        // Use contextual alt text based on surrounding sentences
        String contextualAlt = getContextualAltText();
        
        if (contextualAlt != null && !contextualAlt.isEmpty()) {
            attr.addAttribute("", "alt", "alt", "CDATA", contextualAlt);
        } else if (picDescription != null) {
            attr.addAttribute("", "alt", "alt", "CDATA", picDescription);
        } else if (picFileName != null) {
            attr.addAttribute("", "alt", "alt", "CDATA", picFileName);
        }

        if (xhtml != null) {
            xhtml.startElement("img", attr);
            xhtml.endElement("img");
        }
    }

    @Override
    public void startBookmark(String id, String name) throws SAXException {
        //skip bookmarks within hyperlinks
        if (name != null && !wroteHyperlinkStart) {
            xhtml.startElement("a", "name", name);
            xhtml.endElement("a");
        }
    }

    @Override
    public void endBookmark(String id) {
        //no-op
    }

    private void closeStyleTags() throws SAXException {

        if (isStrikeThrough) {
            xhtml.endElement("strike");
            isStrikeThrough = false;
        }

        if (isUnderline) {
            xhtml.endElement("u");
            isUnderline = false;
        }

        if (isItalics) {
            xhtml.endElement("i");
            isItalics = false;
        }

        if (isBold) {
            xhtml.endElement("b");
            isBold = false;
        }
    }

    private void writeParagraphNumber(int numId, int ilvl, XWPFListManager listManager,
                                      XHTMLContentHandler xhtml) throws SAXException {

        if (ilvl < 0 || numId < 0 || listManager == null) {
            return;
        }
        String number = listManager.getFormattedNumber(BigInteger.valueOf(numId), ilvl);
        if (number != null) {
            xhtml.characters(number);
        }

    }
    
    /**
     * Adds text content to the circular buffer for context tracking
     */
    private void addToTextBuffer(String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        // Add content to buffer
        textBuffer.append(content);
        
        // Keep buffer size limited by removing from the beginning if too large
        if (textBuffer.length() > TEXT_BUFFER_SIZE) {
            int excess = textBuffer.length() - TEXT_BUFFER_SIZE;
            textBuffer.delete(0, excess);
        }
    }
    
    /**
     * Extracts contextual sentences around the current position for use as alt text
     */
    private String getContextualAltText() {
        String text = textBuffer.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        
        // Simple sentence boundary detection - split on sentence endings and paragraph breaks
        String[] sentences = text.split("[.!?]+\\s+|\\n+");
        
        if (sentences.length == 0) {
            return null;
        }
        
        // Take the last sentence(s) - if we have multiple, combine the last two
        // This represents the context around where the image appears
        StringBuilder altText = new StringBuilder();
        
        if (sentences.length >= 2) {
            // Previous sentence + current/next sentence context
            String prevSentence = sentences[sentences.length - 2].trim();
            String nextSentence = sentences[sentences.length - 1].trim();
            
            if (!prevSentence.isEmpty() && !nextSentence.isEmpty()) {
                altText.append(prevSentence).append(" ").append(nextSentence);
            } else if (!prevSentence.isEmpty()) {
                altText.append(prevSentence);
            } else if (!nextSentence.isEmpty()) {
                altText.append(nextSentence);
            }
        } else if (sentences.length == 1) {
            altText.append(sentences[0].trim());
        }
        
        String result = altText.toString().trim();
        
        // If we didn't get a good result with sentence splitting, try paragraph splitting
        if (result.isEmpty() || sentences.length < 2) {
            String[] paragraphs = text.split("\\s{2,}|\\n+");
            if (paragraphs.length >= 2) {
                String prev = paragraphs[paragraphs.length - 2].trim();
                String curr = paragraphs[paragraphs.length - 1].trim();
                if (!prev.isEmpty() && !curr.isEmpty()) {
                    result = prev + " " + curr;
                }
            }
        }
        
        // Limit length to avoid overly long alt text
        if (result.length() > 200) {
            result = result.substring(0, 197) + "...";
        }
        
        return result.isEmpty() ? null : result;
    }
}

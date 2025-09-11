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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.image.ImageGraphicsEngine;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.sax.XHTMLContentHandler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * <p>This was added in Tika 1.24 as an alpha version of a text extractor
 * that builds the text from the marked text tree and includes/normalizes
 * some of the structural tags.
 * </p>
 *
 * @since 1.24
 */

public class PDFMarkedContent2XHTML extends PDF2XHTML {

    private static final int MAX_RECURSION_DEPTH = 1000;
    private static final String DIV = "div";
    private static final Map<String, HtmlTag> COMMON_TAG_MAP = new HashMap<>();

    static {
        //code requires these to be all lower case
        COMMON_TAG_MAP.put("document", new HtmlTag("body"));
        COMMON_TAG_MAP.put("div", new HtmlTag("div"));
        COMMON_TAG_MAP.put("p", new HtmlTag("p"));
        COMMON_TAG_MAP.put("span", new HtmlTag("span"));
        COMMON_TAG_MAP.put("table", new HtmlTag("table"));
        COMMON_TAG_MAP.put("thead", new HtmlTag("thead"));
        COMMON_TAG_MAP.put("tbody", new HtmlTag("tbody"));
        COMMON_TAG_MAP.put("tr", new HtmlTag("tr"));
        COMMON_TAG_MAP.put("th", new HtmlTag("th"));
        COMMON_TAG_MAP.put("td", new HtmlTag("td"));//TODO -- convert to th if in thead?
        COMMON_TAG_MAP.put("l", new HtmlTag("ul"));
        COMMON_TAG_MAP.put("li", new HtmlTag("li"));
        COMMON_TAG_MAP.put("h1", new HtmlTag("h1"));
        COMMON_TAG_MAP.put("h2", new HtmlTag("h2"));
        COMMON_TAG_MAP.put("h3", new HtmlTag("h3"));
        COMMON_TAG_MAP.put("h4", new HtmlTag("h4"));
        COMMON_TAG_MAP.put("h5", new HtmlTag("h5"));
        COMMON_TAG_MAP.put("h6", new HtmlTag("h6"));

    }

    //this stores state as we recurse through the structure tag tree
    private State state = new State();
    
    // Map to collect text positions by page for table detection
    private Map<PDPage, List<TextPosition>> pageTextPositions;
    
    // Map to collect images by page and MCID for proper positioning
    private Map<PDPage, List<ImageInfo>> pageImages;
    private Map<MCID, ImageInfo> mcidToImage;
    
    // Current page being processed
    private PDPage currentPageBeingProcessed;
    
    // Global image counter to ensure unique image names across figures
    private AtomicInteger globalImageCounter = new AtomicInteger(0);
    
    // Keep track of processed images per page to avoid duplicates
    private Map<PDPage, Integer> pageImageIndex = new HashMap<>();

    private PDFMarkedContent2XHTML(PDDocument document, ContentHandler handler,
                                   ParseContext context, Metadata metadata, PDFParserConfig config)
            throws IOException {
        super(document, handler, context, metadata, config);
    }

    /**
     * Converts the given PDF document (and related metadata) to a stream
     * of XHTML SAX events sent to the given content handler.
     *
     * @param pdDocument PDF document
     * @param handler    SAX content handler
     * @param context
     * @param metadata   PDF metadata
     * @param config
     * @throws SAXException  if the content handler fails to process SAX events
     * @throws TikaException if there was an exception outside of per page processing
     */
    public static void process(PDDocument pdDocument, ContentHandler handler,
                               ParseContext context,
                               Metadata metadata, PDFParserConfig config)
            throws SAXException, TikaException {

        PDFMarkedContent2XHTML pdfMarkedContent2XHTML = null;
        try {
            pdfMarkedContent2XHTML =
                    new PDFMarkedContent2XHTML(pdDocument, handler, context, metadata, config);
        } catch (IOException e) {
            throw new TikaException("couldn't initialize PDFMarkedContent2XHTML", e);
        }
        try {
            pdfMarkedContent2XHTML.writeText(pdDocument, new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) {
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        } catch (IOException e) {
            if (e.getCause() instanceof SAXException) {
                throw (SAXException) e.getCause();
            } else {
                throw new TikaException("Unable to extract PDF content", e);
            }
        }
        if (!pdfMarkedContent2XHTML.exceptions.isEmpty()) {
            //throw the first
            throw new TikaException("Unable to extract PDF content",
                    pdfMarkedContent2XHTML.exceptions.get(0));
        }
    }

    private static Map<String, HtmlTag> loadRoleMap(Map<String, Object> roleMap) {
        if (roleMap == null) {
            return Collections.EMPTY_MAP;
        }
        Map<String, HtmlTag> tags = new HashMap<>();
        for (Map.Entry<String, Object> e : roleMap.entrySet()) {
            String k = e.getKey();
            Object obj = e.getValue();
            if (obj instanceof String) {
                String v = (String) obj;
                String lc = v.toLowerCase(Locale.US);
                if (COMMON_TAG_MAP.containsValue(new HtmlTag(lc))) {
                    tags.put(k, new HtmlTag(lc));
                } else {
                    tags.put(k, new HtmlTag(DIV, lc));
                }
            }
        }
        return tags;
    }

    private static void findPages(COSBase kidsObj, List<ObjectRef> pageRefs) {
        if (kidsObj == null) {
            return;
        }
        if (kidsObj instanceof COSArray) {
            for (COSBase kid : ((COSArray) kidsObj)) {
                if (kid instanceof COSObject) {
                    COSBase kidbase = ((COSObject) kid).getObject();
                    if (kidbase instanceof COSDictionary) {
                        COSDictionary dict = (COSDictionary) kidbase;
                        if (COSName.PAGE.equals(dict.getCOSName(COSName.TYPE))) {
                            pageRefs.add(new ObjectRef(((COSObject) kid).getKey().getNumber(),
                                    ((COSObject) kid).getKey().getGeneration()));
                            continue;
                        }
                        if (dict.containsKey(COSName.KIDS)) {
                            findPages(dict.getDictionaryObject(COSName.KIDS), pageRefs);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void processPages(PDPageTree pageTree) throws IOException {

        //this is a 0-indexed list of object refs for each page
        //we need this to map the mcids later...
        //TODO: is there a better way of getting these/doing the mapping?

        List<ObjectRef> pageRefs = new ArrayList<>();
        //STEP 1: get the page refs
        findPages(pageTree.getCOSObject().getDictionaryObject(COSName.KIDS), pageRefs);
        //confirm the right number of pages was found
        if (pageRefs.size() != pdDocument.getNumberOfPages()) {
            throw new IOException(new TikaException(
                    "Couldn't find the right number of page refs (" + pageRefs.size() +
                            ") for pages (" + pdDocument.getNumberOfPages() + ")"));
        }

        PDStructureTreeRoot structureTreeRoot =
                pdDocument.getDocumentCatalog().getStructureTreeRoot();

        //STEP 2: load the roleMap
        Map<String, HtmlTag> roleMap = loadRoleMap(structureTreeRoot.getRoleMap());

        //STEP 3: load all of the text, mapped to MCIDs
        Map<MCID, String> paragraphs = loadTextByMCID(pageTree, pageRefs);

        //STEP 4: now recurse the the structure tree root and output the structure
        //and the text bits from paragraphs

        try {
            recurse(structureTreeRoot.getK(), null, 0, paragraphs, roleMap);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        //STEP 5: handle all the potentially unprocessed bits and integrate tables
        try {
            if (state.hrefAnchorBuilder.length() > 0) {
                xhtml.startElement("p");
                writeString(state.hrefAnchorBuilder.toString());
                xhtml.endElement("p");
            }
            
            // Process tables before unprocessed text to maintain proper document structure
            if (tableDetectionEnabled && pageTextPositions != null) {
                for (PDPage page : pageTree) {
                    if (pageTextPositions.containsKey(page)) {
                        List<TextPosition> textPositions = pageTextPositions.get(page);
                        if (!textPositions.isEmpty()) {
                            List<TableDetector.TableStructure> tables = TableDetector.detectTables(textPositions);
                            for (TableDetector.TableStructure table : tables) {
                                renderTable(table);
                            }
                        }
                    }
                }
            }
            
            for (MCID mcid : paragraphs.keySet()) {
                if (!state.processedMCIDs.contains(mcid)) {
                    if (mcid.mcid > -1) {
                        //TODO: LOG! piece of text that wasn't referenced  in the marked content
                        // tree
                        // but should have been.  If mcid == -1, this was a known item not part of
                        // content tree.
                    }

                    xhtml.startElement("p");
                    writeString(paragraphs.get(mcid));
                    xhtml.endElement("p");
                }
            }
            
            // Note: Images are now extracted inline when figure tags are encountered
            // in the marked content structure, providing proper positioning and
            // maintaining clean document structure.
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (IOException e) {
            handleCatchableIOE(e);
        }
    }
    

    

    

    

    
    @Override
    public void processPage(PDPage page) throws IOException {
        // For marked content processing, we don't use the standard page processing
        // All processing is handled in processPages method through marked content extraction
        // Do nothing here to prevent duplicate processing and content after </body>
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        // For marked content processing, we don't use the standard page lifecycle
        // Page-level processing (annotations, images, etc.) is handled during marked content processing
        // This method should not be called during normal marked content extraction
        // Do nothing to avoid duplicate </body> tags and conflicting page processing
    }

    private void recurse(COSBase kids, ObjectRef currentPageRef, int depth,
                         Map<MCID, String> paragraphs, Map<String, HtmlTag> roleMap)
            throws IOException, SAXException {

        if (depth > MAX_RECURSION_DEPTH) {
            throw new IOException(
                    new TikaException("Exceeded max recursion depth " + MAX_RECURSION_DEPTH));
        }

        if (kids instanceof COSArray) {
            for (COSBase k : ((COSArray) kids)) {
                recurse(k, currentPageRef, depth, paragraphs, roleMap);
            }
        } else if (kids instanceof COSObject && 
                ((COSObject) kids).getObject() instanceof COSDictionary) {
            //TODO should be merged with COSDictionary segment below?
            // and maybe dereference COSObject first, i.e. before the first "if"?
            // No, because we're using the object key for a map
            // However, we could replace ObjectRef with COSBase for currentPageRef. 
            // This way we could also get rid of findPages because that logic is in the
            // iterator of PageTree which we get by calling PDDocument.getPages()
            COSDictionary dict = (COSDictionary) ((COSObject) kids).getObject();
            COSName type = dict.getCOSName(COSName.TYPE);
            if (COSName.OBJR.equals(type)) {
                recurse(dict.getDictionaryObject(COSName.OBJ), currentPageRef, depth + 1, paragraphs,
                        roleMap);
            }

            COSName n = dict.getCOSName(COSName.S);
            String name = "";
            if (n != null) {
                name = ((COSName) n).getName();
            }
            COSBase grandkids = dict.getItem(COSName.K);
            if (grandkids == null) {
                return;
            }
            COSBase pageBase = dict.getItem(COSName.PG);

            if (pageBase instanceof COSObject) {
                currentPageRef = new ObjectRef(((COSObject) pageBase).getKey().getNumber(),
                        ((COSObject) pageBase).getKey().getGeneration());
            }

            HtmlTag tag = getTag(name, roleMap);
            boolean startedLink = false;
            boolean ignoreTag = false;
            boolean isFigure = false;
            
            if ("link".equals(tag.clazz)) {
                state.inLink = true;
                startedLink = true;
            }
            
            // Check if this is a figure tag - check both original name and tag class
            if ("figure".equals(name.toLowerCase()) || "figure".equals(tag.clazz) || 
                name.toLowerCase().contains("figure") || name.toLowerCase().contains("img")) {
                isFigure = true;
                state.inFigure = true;
                state.currentPageRef = currentPageRef;
                state.currentPageBase = pageBase;

            }
            
            if (!state.inLink) {
                //TODO: currently suppressing span and lbody...
                // is this what we want to do?  What else should we suppose?
                if ("span".equals(tag.tag)) {
                    ignoreTag = true;
                } else if ("lbody".equals(tag.clazz)) {
                    ignoreTag = true;
                }
                
                // For figure tags (now mapped to img), we'll handle them specially
                if (isFigure && "img".equals(tag.tag)) {
                    // Don't output the img tag yet - we'll output it with proper attributes
                    ignoreTag = true;
                } else if (!ignoreTag) {
                    if (tag.clazz != null && !tag.clazz.isBlank()) {
                        xhtml.startElement(tag.tag, "class", tag.clazz);
                    } else {
                        xhtml.startElement(tag.tag);
                    }
                }
            }

            recurse(grandkids, currentPageRef, depth + 1, paragraphs, roleMap);
            
            // Handle figure processing - output img tag directly
            if (isFigure && state.inFigure) {
                try {

                    
                    // Extract a single image for this figure and output it as an img tag
                    extractSingleImageAsImgTag(currentPageRef);
                    
                } catch (Exception e) {
                    // Log error but continue processing

                    e.printStackTrace();
                }
            }
            
            if (startedLink) {
                writeLink();
            }
            
            // Reset figure state after processing
            if (isFigure && state.inFigure) {
                state.inFigure = false;
                state.currentPageRef = null;
                state.currentPageBase = null;
            }
            
            // For img tags (self-closing), we don't need to call endElement
            if (!state.inLink && !startedLink && !ignoreTag && !"img".equals(tag.tag)) {
                xhtml.endElement(tag.tag);
            }
        } else if (kids instanceof COSInteger) {
            int mcidInt = ((COSInteger) kids).intValue();
            MCID mcid = new MCID(currentPageRef, mcidInt);
            if (paragraphs.containsKey(mcid)) {
                if (state.inLink) {
                    state.hrefAnchorBuilder.append(paragraphs.get(mcid));
                } else {
                    try {
                        //if it isn't a uri, output this anyhow
                        writeString(paragraphs.get(mcid));
                    } catch (IOException e) {
                        handleCatchableIOE(e);
                    }
                }
                state.processedMCIDs.add(mcid);
            } else {
                //TODO: log can't find mcid
            }
        } else if (kids instanceof COSDictionary) {
            //TODO: check for other types of dictionary?
            COSDictionary dict = (COSDictionary) kids;
            COSDictionary anchor = dict.getCOSDictionary(COSName.A);
            //check for subtype /Link ?
            //COSName subtype = obj.getCOSName(COSName.SUBTYPE);
            if (anchor != null) {
                state.uri = anchor.getString(COSName.URI);
            } else {
                if (dict.containsKey(COSName.K)) {
                    recurse(dict.getDictionaryObject(COSName.K), currentPageRef, depth + 1,
                            paragraphs, roleMap);
                } else if (dict.containsKey(COSName.OBJ)) {
                    recurse(dict.getDictionaryObject(COSName.OBJ), currentPageRef, depth + 1,
                            paragraphs, roleMap);
                }
            }
        } else {
            //TODO: handle a different object?
        }
    }

    private void writeLink() throws SAXException, IOException {
        //This is only for uris, obv.
        //If we want to catch within doc references (GOTO, we need to cache those in state.
        //See testPDF_childAttachments.pdf for examples
        if (state.uri != null && !state.uri.isBlank()) {
            xhtml.startElement("a", "href", state.uri);
            xhtml.characters(state.hrefAnchorBuilder.toString());
            xhtml.endElement("a");
        } else {
            try {
                //if it isn't a uri, output this anyhow
                writeString(state.hrefAnchorBuilder.toString());
            } catch (IOException e) {
                handleCatchableIOE(e);
            }
        }
        state.hrefAnchorBuilder.setLength(0);
        state.inLink = false;
        state.uri = null;

    }

    private HtmlTag getTag(String name, Map<String, HtmlTag> roleMap) {
        if (roleMap.containsKey(name)) {
            return roleMap.get(name);
        }
        String lc = name.toLowerCase(Locale.US);
        if (COMMON_TAG_MAP.containsKey(lc)) {
            return COMMON_TAG_MAP.get(lc);
        }
        
        // Special handling for figure/image tags - map directly to img tags
        if ("figure".equals(lc) || "img".equals(lc)) {
            // Return an img tag that will be processed with actual image attributes
            roleMap.put(name, new HtmlTag("img", null));
            return roleMap.get(name);
        }
        
        roleMap.put(name, new HtmlTag(DIV, name.toLowerCase(Locale.US)));
        return roleMap.get(name);
    }

    private Map<MCID, String> loadTextByMCID(PDPageTree pageTree, List<ObjectRef> pageRefs) throws IOException {
        int pageCount = 1;
        Map<MCID, String> paragraphs = new HashMap<>();
        
        // Map to collect text positions by page for table detection
        pageTextPositions = new HashMap<>();
        
        // Initialize image collection
        pageImages = new HashMap<>();
        mcidToImage = new HashMap<>();
        pageImageIndex = new HashMap<>();
        
        for (PDPage page : pageTree) {
            ObjectRef pageRef = pageRefs.get(pageCount - 1);
            PDFMarkedContentExtractor ex = new PDFMarkedContentExtractor();
            
            // Set current page being processed
            currentPageBeingProcessed = page;
            
            // Initialize text position collection for this page
            List<TextPosition> currentPagePositions = new ArrayList<>();
            pageTextPositions.put(page, currentPagePositions);
            
            try {
                ex.processPage(page);
            } catch (IOException e) {
                handleCatchableIOE(e);
                continue;
            }
            
            // Pre-extract image information for this page
            try {
                preExtractImageInfo(page, pageRef);
            } catch (Exception e) {

            }
            

            for (PDMarkedContent c : ex.getMarkedContents()) {
                //TODO: at some point also handle
                // 1. c.getActualText()
                // 2. c.getExpandedForm()
                // 3. c.getAlternateDescription()
                // 4. c.getLanguage()

                List<Object> objects = c.getContents();
                StringBuilder sb = new StringBuilder();
                //TODO: sort text positions? Figure out when to add/remove a newline and/or space?
                for (Object o : objects) {
                    if (o instanceof TextPosition) {
                        TextPosition textPos = (TextPosition) o;
                        String unicode = textPos.getUnicode();
                        if (unicode != null) {
                            sb.append(unicode);
                            // Collect text positions for table detection if enabled
                            if (tableDetectionEnabled && unicode.trim().length() > 0) {
                                currentPagePositions.add(textPos);
                            }
                        }
                    }
                    /*
                    TODO: do we want to do anything with these?
                    TODO: Are there other types of objects we need to handle here?
                    else if (o instanceof PDImageXObject) {

                    } else if (o instanceof PDTransparencyGroup) {

                    } else if (o instanceof PDMarkedContent) {

                    } else if (o instanceof PDFormXObject) {

                    } else {
                        throw new RuntimeException("can't handle "+o.getClass());
                    }*/
                }

                int mcidInt = c.getMCID();
                MCID mcid = new MCID(pageRef, mcidInt);
                String p = sb.toString();
                if (c.getTag().equals("P")) {
                    p = p.trim();
                }

                if (mcidInt < 0) {
                    //mcidInt == -1 for text bits that do not have an actual
                    //mcid -- concatenate these bits
                    if (paragraphs.containsKey(mcid)) {
                        p = paragraphs.get(mcid) + "\n" + p;
                    }
                }

                paragraphs.put(mcid, p);

            }
            pageCount++;
        }
        
        return paragraphs;
    }
    
    /**
     * Extract images for the current figure context.
     * This method is called when we encounter a figure tag in the marked content structure.
     */
    private void extractImagesForCurrentFigure() throws SAXException, IOException {
        if (!config.isExtractInlineImages() && !config.isExtractInlineImageMetadataOnly()) {
            return;
        }
        
        // We need to find the current page from the page reference
        PDPage currentPage = findPageFromRef(state.currentPageRef);
        if (currentPage == null) {
            return;
        }
        
        try {
            // Use the same image extraction logic as PDF2XHTML
            // This will generate proper <img src="embedded:filename" alt="description" /> tags
            // or <img src="data:image/jpeg;base64,xxxxx" alt="filename" /> if convertEmbeddedImagesToBase64 is true
            ImageGraphicsEngine engine = config.getImageGraphicsEngineFactory().newEngine(
                    currentPage, getCurrentPageNo(), embeddedDocumentExtractor, config,
                    new HashMap<>(), new AtomicInteger(0), xhtml, metadata, context);
            engine.run();
            List<IOException> engineExceptions = engine.getExceptions();
            if (!engineExceptions.isEmpty()) {
                IOException first = engineExceptions.remove(0);
                if (config.isCatchIntermediateIOExceptions()) {
                    exceptions.addAll(engineExceptions);
                }
                throw first;
            }
        } catch (Exception e) {
            // Log error but continue processing to avoid breaking the entire document

        }
    }
    
    /**
     * Pre-extracts image information for a page and associates them with MCIDs.
     * This method runs before text extraction to collect image metadata.
     */
    private void preExtractImageInfo(PDPage page, ObjectRef pageRef) throws IOException {
        if (page == null || !config.isExtractInlineImages()) {

            return;
        }
        

        
        // Initialize maps if needed
        if (pageImages == null) {
            pageImages = new HashMap<>();
        }
        if (mcidToImage == null) {
            mcidToImage = new HashMap<>();
        }
        
        List<ImageInfo> images = new ArrayList<>();
        pageImages.put(page, images);
        
        // Create a custom ImageGraphicsEngine that collects image info instead of outputting
        ImageInfoCollector collector = new ImageInfoCollector(page, getCurrentPageNo(), 
                embeddedDocumentExtractor, config, new HashMap<>(), new AtomicInteger(0), 
                pageRef, images, mcidToImage);
        collector.run();
        

    }
    
    /**
     * Outputs images that correspond to the current figure's MCID.
     * This ensures each figure only shows its associated images.
     */
    private void outputImagesForCurrentFigure(ObjectRef pageRef, COSArray grandkids) throws IOException, SAXException {
        if (pageRef == null) {

            return;
        }
        

        
        // Find the MCID for this figure by examining its children
        Set<Integer> figureMcids = new HashSet<>();
        if (grandkids != null) {
            collectMcidsFromArray(grandkids, figureMcids);
        }

        
        // If no specific MCIDs found or grandkids is null, try to output any available images for this page
        if ((figureMcids.isEmpty() || grandkids == null) && pageImages != null) {
            PDPage page = findPageFromRef(pageRef);
            if (page != null && pageImages.containsKey(page)) {
                List<ImageInfo> images = pageImages.get(page);

                for (ImageInfo imageInfo : images) {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("", "src", "src", "CDATA", imageInfo.getSrc());
                    if (imageInfo.getAlt() != null && !imageInfo.getAlt().isEmpty()) {
                        attrs.addAttribute("", "alt", "alt", "CDATA", imageInfo.getAlt());
                    }
                    xhtml.startElement("img", attrs);
                    xhtml.endElement("img");

                }
                return;
            }
        }
        
        // Output only images associated with these MCIDs
        int outputCount = 0;
        for (Integer mcidInt : figureMcids) {
            MCID mcid = new MCID(pageRef, mcidInt);
            ImageInfo imageInfo = mcidToImage.get(mcid);
            if (imageInfo != null) {
                // Output the image
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "src", "src", "CDATA", imageInfo.getSrc());
                if (imageInfo.getAlt() != null && !imageInfo.getAlt().isEmpty()) {
                    attrs.addAttribute("", "alt", "alt", "CDATA", imageInfo.getAlt());
                }
                xhtml.startElement("img", attrs);
                xhtml.endElement("img");
                outputCount++;

            } else {

            }
        }

    }
    
    /**
     * Recursively collects MCIDs from a COSArray structure.
     */
    private void collectMcidsFromArray(COSArray array, Set<Integer> mcids) {
        for (COSBase item : array) {
            if (item instanceof COSInteger) {
                mcids.add(((COSInteger) item).intValue());
            } else if (item instanceof COSArray) {
                collectMcidsFromArray((COSArray) item, mcids);
            } else if (item instanceof COSDictionary) {
                COSDictionary dict = (COSDictionary) item;
                COSBase kids = dict.getDictionaryObject(COSName.K);
                if (kids instanceof COSArray) {
                    collectMcidsFromArray((COSArray) kids, mcids);
                } else if (kids instanceof COSInteger) {
                    mcids.add(((COSInteger) kids).intValue());
                }
            }
        }
    }
    
    /**
     * Extract a single image and output it as an img tag with proper attributes
     */
    private void extractSingleImageAsImgTag(ObjectRef pageRef) throws IOException, SAXException {
        if (pageRef == null) {

            return;
        }
        
        PDPage currentPage = findPageFromRef(pageRef);
        if (currentPage == null) {

            return;
        }
        
        try {
            // Get the current image index for this page
            int currentImageIndex = pageImageIndex.getOrDefault(currentPage, 0);
            
            // First, let's count how many images are actually on this page
            int totalImagesOnPage = countImagesOnPage(currentPage);
            
            // Handle cases where there are no images or we need to cycle through available images
            if (totalImagesOnPage == 0) {
                return; // Skip this figure
            }
            
            int actualImageIndex = currentImageIndex;
            if (currentImageIndex >= totalImagesOnPage) {
                actualImageIndex = currentImageIndex % totalImagesOnPage;
            }
            
            // Create a custom image extractor that only outputs one specific image as img tag
            DirectImageExtractor extractor = new DirectImageExtractor(
                currentPage, getCurrentPageNo(), embeddedDocumentExtractor, config,
                new HashMap<>(), globalImageCounter, xhtml, metadata, context, actualImageIndex);
            
            extractor.run();
            
            // Update the image index for this page
            int newIndex = currentImageIndex + 1;
            pageImageIndex.put(currentPage, newIndex);
            

        } catch (Exception e) {
            // Log error but continue processing to avoid breaking the entire document

            e.printStackTrace();
        }
    }
    
    /**
     * Extract images for the current page directly using existing logic
     */
    private void extractImagesForCurrentPage(ObjectRef pageRef) throws IOException, SAXException {
        if (pageRef == null) {

            return;
        }
        
        PDPage currentPage = findPageFromRef(pageRef);
        if (currentPage == null) {

            return;
        }
        

        
        try {
            // Get the current image index for this page
            int currentImageIndex = pageImageIndex.getOrDefault(currentPage, 0);

            
            // Create a custom image extractor that skips previous images
            SingleImageExtractor extractor = new SingleImageExtractor(
                currentPage, getCurrentPageNo(), embeddedDocumentExtractor, config,
                new HashMap<>(), globalImageCounter, xhtml, metadata, context, currentImageIndex);
            
            extractor.run();
            
            // Update the image index for this page
            pageImageIndex.put(currentPage, currentImageIndex + 1);
            

        } catch (Exception e) {
            // Log error but continue processing to avoid breaking the entire document

            e.printStackTrace();
        }
    }
    
    /**
     * Count the total number of images on a page
     */
    private int countImagesOnPage(PDPage page) {
        if (page == null) {
            return 0;
        }
        
        try {
            ImageCounter counter = new ImageCounter();
            ImageGraphicsEngine engine = config.getImageGraphicsEngineFactory().newEngine(
                    page, getCurrentPageNo(), embeddedDocumentExtractor, config,
                    new HashMap<>(), new AtomicInteger(0), null, metadata, context);
            
            // We need to create a custom counter that doesn't output anything
            ImageCounterEngine counterEngine = new ImageCounterEngine(
                page, getCurrentPageNo(), embeddedDocumentExtractor, config,
                new HashMap<>(), new AtomicInteger(0), counter);
            counterEngine.run();
            
            return counter.getCount();
        } catch (Exception e) {

            return 0;
        }
    }
    
    /**
     * Simple image counter class
     */
    private static class ImageCounter {
        private int count = 0;
        public void increment() { count++; }
        public int getCount() { return count; }
    }
    
    /**
     * ImageGraphicsEngine that only counts images without outputting them
     */
    private class ImageCounterEngine extends ImageGraphicsEngine {
        private final ImageCounter counter;
        
        public ImageCounterEngine(PDPage page, int pageNumber, 
                                EmbeddedDocumentExtractor embeddedDocumentExtractor,
                                PDFParserConfig config, Map<COSStream, Integer> processedInlineImages,
                                AtomicInteger imageCounter, ImageCounter counter) {
            super(page, pageNumber, embeddedDocumentExtractor, config, processedInlineImages, 
                  imageCounter, null, null, null);
            this.counter = counter;
        }
        
        @Override
        protected void processImage(PDImage pdImage, int imageNumber) throws IOException {
            counter.increment();
            // Don't call super.processImage() - we're just counting
        }
    }
    
    /**
     * Find the PDPage object from an ObjectRef.
     * This is a simplified implementation - in a full implementation,
     * we would need to properly map ObjectRefs to PDPage objects.
     */
    private PDPage findPageFromRef(ObjectRef pageRef) {
        if (pageRef == null) {
            return null;
        }
        
        // Return the current page being processed
        if (currentPageBeingProcessed != null) {
            return currentPageBeingProcessed;
        }
        
        // Fallback: return the first page
        try {
            PDPageTree pageTree = pdDocument.getPages();
            if (pageTree.getCount() > 0) {
                return pageTree.get(0);
            }
        } catch (Exception e) {

        }
        return null;
    }
    private static class State {
        Set<MCID> processedMCIDs = new HashSet<>();
        boolean inLink = false;
        boolean inFigure = false;
        int tableDepth = 0;
        private StringBuilder hrefAnchorBuilder = new StringBuilder();
        private String uri = null;
        private int tdDepth = 0;
        // Store current page context for figure processing
        ObjectRef currentPageRef = null;
        COSBase currentPageBase = null;
    }

    private static class HtmlTag {
        private final String tag;
        private final String clazz;

        HtmlTag() {
            this("");
        }

        HtmlTag(String tag) {
            this(tag, "");
        }

        HtmlTag(String tag, String clazz) {
            this.tag = tag;
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HtmlTag htmlTag = (HtmlTag) o;

            if (!Objects.equals(tag, htmlTag.tag)) {
                return false;
            }
            return Objects.equals(clazz, htmlTag.clazz);
        }

        @Override
        public int hashCode() {
            int result = tag != null ? tag.hashCode() : 0;
            result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
            return result;
        }
    }

    private static class ObjectRef {
        private final long objId;
        private final int version;

        public ObjectRef(long objId, int version) {
            this.objId = objId;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ObjectRef objectRef = (ObjectRef) o;
            return objId == objectRef.objId && version == objectRef.version;
        }

        @Override
        public int hashCode() {
            return Objects.hash(objId, version);
        }

        @Override
        public String toString() {
            return "ObjectRef{" + "objId=" + objId + ", version=" + version + '}';
        }
    }

    /**
     * In PDF land, MCID are integers that should be unique _per page_.
     * This class includes the object ref to the page and the mcid
     * so that this should be a cross-document unique key to
     * given content.
     * <p>
     * If the mcid integer == -1, that means that there is text on the page
     * not assigned to any marked content.
     */
    private static class MCID {
        //this is the object ref to the particular page
        private final ObjectRef objectRef;
        private final int mcid;

        public MCID(ObjectRef objectRef, int mcid) {
            this.objectRef = objectRef;
            this.mcid = mcid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MCID mcid1 = (MCID) o;
            return mcid == mcid1.mcid && Objects.equals(objectRef, mcid1.objectRef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectRef, mcid);
        }

        @Override
        public String toString() {
            return "MCID{" + "objectRef=" + objectRef + ", mcid=" + mcid + '}';
        }
    }
    
    /**
     * Custom ImageGraphicsEngine that outputs a single image directly as an img tag.
     * This is used when figure tags are mapped directly to img tags.
     */
    private class DirectImageExtractor extends ImageGraphicsEngine {
        private final int targetImageIndex;
        private int currentImageIndex = 0;
        
        public DirectImageExtractor(PDPage page, int pageNumber, 
                                  EmbeddedDocumentExtractor embeddedDocumentExtractor,
                                  PDFParserConfig config, Map<COSStream, Integer> processedInlineImages,
                                  AtomicInteger imageCounter, XHTMLContentHandler xhtml,
                                  Metadata metadata, ParseContext context, int targetImageIndex) {
            super(page, pageNumber, embeddedDocumentExtractor, config, processedInlineImages, 
                  imageCounter, xhtml, metadata, context);
            this.targetImageIndex = targetImageIndex;
        }
        
        @Override
        protected void processImage(PDImage pdImage, int imageNumber) throws IOException {

            
            // Only process the image at our target index
            if (currentImageIndex == targetImageIndex) {

                try {
                    super.processImage(pdImage, imageNumber);

                } catch (Exception e) {

                    // Continue processing other images
                }
            } else {

            }
            
            currentImageIndex++;
        }
    }
    
    /**
     * Custom ImageGraphicsEngine that extracts only a single specific image.
     * This ensures each figure gets a different image.
     */
    private class SingleImageExtractor extends ImageGraphicsEngine {
        private final int targetImageIndex;
        private int currentImageIndex = 0;
        
        public SingleImageExtractor(PDPage page, int pageNumber, 
                                  EmbeddedDocumentExtractor embeddedDocumentExtractor,
                                  PDFParserConfig config, Map<COSStream, Integer> processedInlineImages,
                                  AtomicInteger imageCounter, XHTMLContentHandler xhtml,
                                  Metadata metadata, ParseContext context, int targetImageIndex) {
            super(page, pageNumber, embeddedDocumentExtractor, config, processedInlineImages, 
                  imageCounter, xhtml, metadata, context);
            this.targetImageIndex = targetImageIndex;
        }
        
        @Override
        protected void processImage(PDImage pdImage, int imageNumber) throws IOException {

            
            // Only process the image at our target index
            if (currentImageIndex == targetImageIndex) {

                try {
                    super.processImage(pdImage, imageNumber);
                } catch (Exception e) {

                    // Continue processing other images
                }
            } else {

            }
            
            currentImageIndex++;
        }
    }
    
    /**
     * Custom ImageGraphicsEngine that collects image information instead of outputting images.
     * This is used during the pre-extraction phase to build MCID-to-image mappings.
     */
    private class ImageInfoCollector extends ImageGraphicsEngine {
        private final ObjectRef pageRef;
        private final List<ImageInfo> images;
        private final Map<MCID, ImageInfo> mcidToImage;
        private int imageCounterLocal = 0;
        
        public ImageInfoCollector(PDPage page, int pageNumber, 
                                EmbeddedDocumentExtractor embeddedDocumentExtractor,
                                PDFParserConfig config, Map<COSStream, Integer> processedInlineImages,
                                AtomicInteger imageCounter, ObjectRef pageRef,
                                List<ImageInfo> images, Map<MCID, ImageInfo> mcidToImage) {
            super(page, pageNumber, embeddedDocumentExtractor, config, processedInlineImages, imageCounter, 
                  null, null, null); // No XHTMLContentHandler needed for collection
            this.pageRef = pageRef;
            this.images = images;
            this.mcidToImage = mcidToImage;
        }
        
        @Override
        protected void processImage(PDImage pdImage, int imageNumber) throws IOException {

            
            // Instead of outputting the image, collect its information
            String imageName = "image" + imageCounterLocal;
            String src;
            String alt = imageName; // Use image name as alt
            
            if (pdfParserConfig.isConvertEmbeddedImagesToBase64()) {
                // Convert to base64
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    BufferedImage bufferedImage = pdImage.getImage();
                    ImageIO.write(bufferedImage, "png", baos);
                    byte[] imageBytes = baos.toByteArray();
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    src = "data:image/png;base64," + base64;
                }
            } else {
                // Use embedded reference
                src = "embedded:" + imageName + ".png";
            }
            
            // For now, associate with a generic MCID - this would need more sophisticated
            // mapping in a full implementation to determine which MCID corresponds to which image
            MCID mcid = new MCID(pageRef, imageCounterLocal);
            
            ImageInfo imageInfo = new ImageInfo(src, alt, mcid);
            images.add(imageInfo);
            mcidToImage.put(mcid, imageInfo);
            

            
            imageCounterLocal++;
        }
    }
    
    /**
     * Information about an image found in the PDF
     */
    private static class ImageInfo {
        private final String src;
        private final String alt;
        private final MCID mcid;
        
        public ImageInfo(String src, String alt, MCID mcid) {
            this.src = src;
            this.alt = alt;
            this.mcid = mcid;
        }
        
        public String getSrc() { return src; }
        public String getAlt() { return alt; }
        public MCID getMcid() { return mcid; }
    }
}

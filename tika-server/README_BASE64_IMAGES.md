# Tika Server Base64 Image Embedding Feature

## Problem Statement

When using Tika Server to extract HTML content from documents containing embedded images, the output contains image references like:

```html
<img src="embedded:image5.png" alt="image5.png" />
```

This requires clients to make additional API calls to retrieve each embedded image separately, which is inefficient and complicates client-side processing.

## Solution

This implementation adds a new content handler `EmbeddedImageBase64ContentHandler` that automatically converts embedded image references to base64 data URLs for HTML output. 

### Key Features

1. **Automatic Image Capture**: During document parsing, the handler captures embedded image data in memory
2. **Base64 Conversion**: Image data is converted to base64 format and embedded as data URLs
3. **MIME Type Detection**: Proper MIME types are detected based on file extensions
4. **HTML-Only Processing**: The conversion only applies to HTML output format
5. **Fallback Behavior**: If conversion fails, the original `embedded:` URL is preserved

### Example Output

**Before:**
```html
<img src="embedded:image5.png" alt="image5.png" />
```

**After:**
```html
<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" alt="image5.png" />
```

### Implementation Details

The solution consists of two main components:

1. **EmbeddedImageBase64ContentHandler**: A SAX content handler decorator that:
   - Intercepts embedded document extraction to capture image data
   - Processes HTML img elements with `embedded:` src attributes
   - Converts captured image data to base64 data URLs

2. **Modified TikaResource**: Updated to use the new handler for HTML output format

### Usage

The feature is automatically enabled for HTML output from the Tika Server. No configuration is required.

**API Usage:**
```bash
curl -X PUT -T document.docx -H "Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document" \
     -H "Accept: text/html" \
     http://localhost:9998/tika
```

The returned HTML will contain embedded images as base64 data URLs instead of `embedded:` references.

### Benefits

- **Single API Call**: Clients can retrieve complete HTML with images in one request
- **Simplified Processing**: No need to handle separate image retrieval logic
- **Better Performance**: Reduces the number of HTTP requests needed
- **Self-Contained Output**: HTML output is completely self-contained with embedded images

### Supported Image Formats

- PNG (.png)
- JPEG (.jpg, .jpeg)
- GIF (.gif)
- BMP (.bmp)
- TIFF (.tif, .tiff)
- WebP (.webp)
- SVG (.svg)

### Compatibility

This feature:
- Only affects HTML output from the Tika Server
- Does not change XML, JSON, or plain text output formats
- Is backward compatible with existing clients
- Gracefully falls back to original behavior if image conversion fails
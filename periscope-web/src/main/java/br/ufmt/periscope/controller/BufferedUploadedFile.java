package br.ufmt.periscope.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.primefaces.model.file.UploadedFile;

/**
 * In-memory {@link UploadedFile} that survives across JSF AJAX requests.
 * <p>
 * PrimeFaces {@code native} uploader wraps {@code jakarta.servlet.http.Part},
 * which is request-scoped; keeping that wrapper in a {@code @ViewScoped} bean
 * and reading it on a later request yields {@link IOException}.
 */
final class BufferedUploadedFile implements UploadedFile, Serializable {

    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final String contentType;
    private final byte[] content;

    BufferedUploadedFile(UploadedFile source) throws IOException {
        this.fileName = source.getFileName();
        this.contentType = source.getContentType();
        byte[] bytes = source.getContent();
        this.content = bytes != null ? bytes : new byte[0];
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getWebkitRelativePath() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public void write(String filePath) throws Exception {
        throw new UnsupportedOperationException("BufferedUploadedFile is read-only");
    }

    @Override
    public void delete() {
        // no-op: content lives on the heap only
    }
}

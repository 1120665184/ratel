package org.quyq.gwsu.common.api.resolver;

import org.springframework.core.io.AbstractResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public class MultipartFileResource extends AbstractResource {

    private final MultipartFile multipartFile;

    public MultipartFileResource(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }

    @Override
    public String getFilename() {
        return multipartFile.getOriginalFilename();
    }

    @Override
    public long contentLength() throws IOException {
        return multipartFile.getSize();
    }

    @Override
    public boolean exists() {
        return !multipartFile.isEmpty();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return multipartFile.getInputStream();
    }

    @Override
    public String getDescription() {
        return "MultipartFile resource [" + multipartFile.getOriginalFilename() + "]";
    }
}

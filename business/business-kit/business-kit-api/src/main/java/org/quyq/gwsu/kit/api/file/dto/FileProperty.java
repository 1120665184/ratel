package org.quyq.gwsu.kit.api.file.dto;

import lombok.Getter;
import org.quyq.gwsu.kit.api.file.enums.FileScope;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class FileProperty {

    private Boolean disposable;

    private FileScope scope;

    private String visitors;

    private String categorize;

    private LocalDateTime expiredTime;

    private FileProperty() {
    }

    public static FilePropertyBuilder builder() {
        return new FilePropertyBuilder();
    }

    public static class FilePropertyBuilder {

        private final FileProperty property;

        private FilePropertyBuilder() {
            this.property = new FileProperty();
            this.property.disposable = false;
            this.property.scope = FileScope.PROTECTED;
        }

        public FilePropertyBuilder isDisposable() {
            this.property.disposable = true;
            return this;
        }

        public FilePropertyBuilder expiredTime(LocalDateTime expiredTime) {
            this.property.expiredTime = expiredTime;
            return this;
        }

        public FilePropertyBuilder scopePublic() {
            this.property.scope = FileScope.PUBLIC;
            return this;
        }

        public FilePropertyBuilder scopeProtected() {
            this.property.scope = FileScope.PROTECTED;
            return this;
        }

        public FilePropertyBuilder scopePrivate(List<String> visitors) {
            this.property.scope = FileScope.PRIVATE;
            this.property.visitors = String.join(",", visitors);
            return this;
        }

        public FilePropertyBuilder categorize(String categorize) {
            this.property.categorize = categorize;
            return this;
        }

        public FileProperty build() {
            return this.property;
        }

    }

}

import org.apache.commons.io.FilenameUtils
import org.gradle.api.Action
import org.gradle.api.Transformer
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath

class SplitRenameAction implements Action<FileCopyDetails> {
    private final Transformer<String, String> transformer

    SplitRenameAction(Transformer<String, String> transformer) {
        this.transformer = transformer
    }

    void execute(FileCopyDetails fileCopyDetails) {
        RelativePath path = fileCopyDetails.getRelativePath()
        String newName = transformer.transform(path.getLastName())
        if (newName != null) {
            path = path.replaceLastName(FilenameUtils.removeExtension(newName))
            fileCopyDetails.setRelativePath(path)
        }
    }
}
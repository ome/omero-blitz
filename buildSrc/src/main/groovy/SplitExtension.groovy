class SplitExtension {
    final String name
    Language language
    File outputDir
    File combinedDir

    def language(String language) {
        this.language = Language.valueOf(language.trim().toUpperCase())
    }

    def outputDir(String dir) {
        this.outputDir = new File(dir)
    }

    def combinedDir(String dir) {
        this.combinedDir = new File(dir)
    }

    def combinedDir(File dir) {
        this.combinedDir = dir
    }

    SplitExtension(String name) {
        this.name = name
    }
}
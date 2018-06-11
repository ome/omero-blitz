class BlitzExtension {
    File mappingsDir
    File combinedDir

    def mappingsDir(String dir) {
        this.mappingsDir = new File(dir)
    }

    def mappingsDir(File dir) {
        this.mappingsDir = dir
    }

    def combinedDir(String dir) {
        this.combinedDir = new File(dir)
    }

    def combinedDir(File dir) {
        this.combinedDir = dir
    }
}
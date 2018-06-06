enum Language {
    CPP(Prefix.HDR, Prefix.CPP),
    JAVA(Prefix.JAV),
    PYTHON(Prefix.PYC),
    ICE(Prefix.ICE)

    static def find(String language) {
        def lang = language.trim().toUpperCase()
        for (def sl : values()) {
            if (sl.name() == lang) {
                return sl
            }
        }
        return null
    }

    Language(Prefix... prefixes) {
        this.prefixes = prefixes
    }

    Prefix[] prefixes
}
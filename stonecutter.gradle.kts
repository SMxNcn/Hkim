plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter {
    flags {
        set("line_separator", "\n")
    }
}

// Change with the Stonecutter Gradle task / IDE plugin, not by hand.
stonecutter active "26.2"

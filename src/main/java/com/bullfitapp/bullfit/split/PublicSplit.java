package com.bullfitapp.bullfit.split;

public record PublicSplit(SplitView split, String publisher, String publisherColor,
        String publisherPrefix, String prefixColor,
        long copyCount, boolean mine, boolean alreadySaved) {
}

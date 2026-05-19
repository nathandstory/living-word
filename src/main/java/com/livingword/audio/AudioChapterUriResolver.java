package com.livingword.audio;

import java.io.IOException;
import java.net.URI;

public interface AudioChapterUriResolver {
    URI resolve(AudioManifest manifest, AudioChapterId chapterId) throws IOException;
}

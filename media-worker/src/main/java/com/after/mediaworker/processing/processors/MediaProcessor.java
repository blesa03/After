package com.after.mediaworker.processing.processors;

import com.after.mediaworker.processing.MediaInfo;
import java.nio.file.Path;

public interface MediaProcessor {
    boolean supports(String type);
    void process(MediaInfo info, Path localOriginalFile, Path workspace) throws Exception;
}
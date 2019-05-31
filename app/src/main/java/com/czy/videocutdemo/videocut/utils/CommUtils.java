package com.czy.videocutdemo.videocut.utils;

import android.media.MediaMetadataRetriever;

public class CommUtils {
    /**
     * 获取视频时长
     */
    public static Long getVideoDuring(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        Long during = 0L;
        try {
            during = Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (NumberFormatException e) {
            during = -1L;
        }

        return during;
    }
}

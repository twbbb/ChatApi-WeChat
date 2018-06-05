package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.XRequest;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

final class WeChatTools {
    public static String fileType(File file) {
        switch (WeChatTools.fileSuffix(file)) {
            case "bmp":
            case "png":
            case "jpeg":
            case "jpg":
                return "pic";
            case "mp4":
                return "video";
            default:
                return "doc";
        }
    }

    public static String fileSuffix(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf('.') > 0) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        } else {
            return "";
        }
    }

    public static class MultipartContent extends XRequest.ParamsContent {

        public MultipartContent() {
            this.boundary = XTools.md5(String.format("multipart-%d-%d", System.currentTimeMillis(), new Random().nextInt()));
        }

        @Override
        public String contentType() {
            return XRequest.MIME_MULTIPART + "; boundary=" + boundary;
        }

        @Override
        public long contentLength() {
            try {
                long contentLength = 0;
                for (XRequest.KeyValue keyValue : params) {
                    if (keyValue.value instanceof Slice) {
                        Slice slice = (Slice) keyValue.value;
                        contentLength += (MINUS + boundary + CRLF).getBytes("utf-8").length;
                        contentLength += String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", keyValue.key, slice.fileName, CRLF).getBytes("utf-8").length;
                        contentLength += String.format("Content-Type: %s%s", slice.fileMime, CRLF).getBytes("utf-8").length;
                        contentLength += CRLF.getBytes("utf-8").length;
                        contentLength += slice.count;
                        contentLength += CRLF.getBytes("utf-8").length;
                    } else if (keyValue.value instanceof File) {
                        File file = (File) keyValue.value;
                        contentLength += (MINUS + boundary + CRLF).getBytes("utf-8").length;
                        contentLength += String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", keyValue.key, file.getName(), CRLF).getBytes("utf-8").length;
                        contentLength += String.format("Content-Type: %s%s", URLConnection.getFileNameMap().getContentTypeFor(file.getAbsolutePath()), CRLF).getBytes("utf-8").length;
                        contentLength += CRLF.getBytes("utf-8").length;
                        contentLength += file.length();
                        contentLength += CRLF.getBytes("utf-8").length;
                    } else {
                        contentLength += (MINUS + boundary + CRLF).getBytes("utf-8").length;
                        contentLength += String.format("Content-Disposition: form-data; name=\"%s\"%s", keyValue.key, CRLF).getBytes("utf-8").length;
                        contentLength += CRLF.getBytes("utf-8").length;
                        contentLength += String.valueOf(keyValue.value).getBytes("utf-8").length;
                        contentLength += CRLF.getBytes("utf-8").length;
                    }
                }
                contentLength = contentLength + (MINUS + boundary + MINUS + CRLF).getBytes("utf-8").length;
                return contentLength;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public void contentWrite(DataOutputStream doStream) throws Exception {
            for (XRequest.KeyValue keyValue : params) {
                if (keyValue.value instanceof Slice) {
                    Slice slice = (Slice) keyValue.value;
                    doStream.write((MINUS + boundary + CRLF).getBytes("utf-8"));
                    doStream.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", keyValue.key, slice.fileName, CRLF).getBytes("utf-8"));
                    doStream.write(String.format("Content-Type: %s%s", slice.fileMime, CRLF).getBytes("utf-8"));
                    doStream.write(CRLF.getBytes("utf-8"));
                    doStream.write(slice.slice, 0, slice.count);
                    doStream.write(CRLF.getBytes("utf-8"));
                } else if (keyValue.value instanceof File) {
                    File file = (File) keyValue.value;
                    doStream.write((MINUS + boundary + CRLF).getBytes("utf-8"));
                    doStream.write(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"%s", keyValue.key, file.getName(), CRLF).getBytes("utf-8"));
                    doStream.write(String.format("Content-Type: %s%s", Files.probeContentType(Paths.get(file.getAbsolutePath())), CRLF).getBytes("utf-8"));
                    doStream.write(CRLF.getBytes("utf-8"));
                    try (FileInputStream fiStream = new FileInputStream(file)) {
                        XTools.streamToStream(fiStream, doStream);
                    }
                    doStream.write(CRLF.getBytes("utf-8"));
                } else {
                    doStream.write((MINUS + boundary + CRLF).getBytes("utf-8"));
                    doStream.write(String.format("Content-Disposition: form-data; name=\"%s\"%s", keyValue.key, CRLF).getBytes("utf-8"));
                    doStream.write(CRLF.getBytes("utf-8"));
                    doStream.write(String.valueOf(keyValue.value).getBytes("utf-8"));
                    doStream.write(CRLF.getBytes("utf-8"));
                }
            }
            doStream.write((MINUS + boundary + MINUS + CRLF).getBytes("utf-8"));
        }
    }

    public static final class Slice {
        public String fileName;
        public String fileMime;
        public byte[] slice;
        public int count;

        public Slice(String fileName, String fileMime, byte[] slice, int count) {
            this.fileName = fileName;
            this.fileMime = fileMime;
            this.slice = slice;
            this.count = count;
        }
    }
}

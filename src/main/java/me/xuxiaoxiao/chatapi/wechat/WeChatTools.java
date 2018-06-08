package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.XRequest;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

final class WeChatTools {
    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

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
        try (FileInputStream is = new FileInputStream(file)) {
            byte[] b = new byte[3];
            is.read(b, 0, b.length);
            String fileCode = bytesToHex(b);

            switch (fileCode) {
                case "ffd8ff":
                    return "jpg";
                case "89504e":
                    return "png";
                case "474946":
                    return "gif";
                default:
                    if (fileCode.startsWith("424d")) {
                        return "bmp";
                    } else if (file.getName().lastIndexOf('.') > 0) {
                        return file.getName().substring(file.getName().lastIndexOf('.') + 1);
                    } else {
                        return "";
                    }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将字节数组转换成16进制字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的字符串，全小写字母
     */
    private static String bytesToHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            chars[i << 1] = HEX[b >>> 4 & 0xf];
            chars[(i << 1) + 1] = HEX[b & 0xf];
        }
        return new String(chars);
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

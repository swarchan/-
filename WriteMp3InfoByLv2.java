import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * 参考：https://blog.csdn.net/zgcqflqinhao/article/details/129492621
 * 原理：使用IO流(RandomAccessFile)，修改ID3V2
 * 结论：文件属性修改成功，QQ音乐也没修改成功
 */
public class WriteMp3InfoByLv2 {
    private static final String TAG = WriteMp3InfoByLv2.class.getSimpleName();
    private static final Charset sCharset = Charset.forName("GBK");
    private static final int ID3V1_TAG_LENGTH = 128;
    private static final String ID3V1_TAG_START = "TAG";
    private static final String ID3V2_TAG_START = "ID3";

    public static void main(String[] args) throws IOException {
        String fileName = "林俊杰-加油.mp3";
        String tmp = "44";
        String newTitle = fileName.substring(0, fileName.lastIndexOf("."));
        String singerName = newTitle.split("-")[0] + tmp;
        String songName = newTitle.split("-")[1] + tmp;
        String album = "100天" + tmp;

        File src = new File("resources2/" + fileName);
        File albumImg = null;//new File("resources1/林俊杰.jpg");
        removeID3V2(src);
        setID3V2(src, songName, singerName, album, albumImg);
    }

    public static byte[] reverse(byte[] origin) {
        for (int i = 0; i < origin.length / 2; i++) {
            byte temp = origin[i];
            origin[i] = origin[origin.length - i - 1];
            origin[origin.length - i - 1] = temp;
        }
        return origin;
    }

    private static byte[] int2Bytes(int i) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (i & 0xFF);
        byteArray[1] = (byte) ((i & 0xFF00) >> 8);
        byteArray[2] = (byte) ((i & 0xFF0000) >> 16);
        byteArray[3] = (byte) ((i & 0xFF000000) >> 24);
        return byteArray;
    }

    private static int bytes2Int(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return 0;
        }
        return (0xFF & bytes[0]) | (0xFF00 & (bytes[1] << 8)) | (0xFF0000 & (bytes[2] << 16)) | (0xFF000000 & (bytes[3] << 24));
    }

    private static int syncIntEncode(int value) {
        int result = 0;
        int mask = 0x7F;
        while ((mask ^ 0x7FFFFFFF) > 0) {
            result = value & ~mask;
            result <<= 1;
            result |= value & mask;
            mask = ((mask + 1) << 8) - 1;
            value = result;
        }
        return result;
    }

    private static int syncIntDecode(int value) {
        int a = 0;
        int b = 0;
        int c = 0;
        int d = 0;
        int result = 0x0;
        a = value & 0xFF;
        b = (value >> 8) & 0xFF;
        c = (value >> 16) & 0xFF;
        d = (value >> 24) & 0xFF;

        result = result | a;
        result = result | (b << 7);
        result = result | (c << 14);
        result = result | (d << 21);
        return result;
    }

    private static class Frame {
        private String mId;
        private int mSize;
        private byte[] mFlag;
        private byte[] mContent;

        public Frame() {
        }

        public Frame(String id, int size, byte[] flag, byte[] content) {
            mId = id;
            mSize = size;
            mFlag = flag;
            mContent = content;
        }
    }

    public static void setID3V2(File src, String title, String artist, String album, File albumImg) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        randomAccessFile.seek(0);
        byte[] bytes = new byte[3];
        randomAccessFile.read(bytes);
        String tag = new String(bytes);
        Map<String, Frame> frames = new HashMap<>();
        if (ID3V2_TAG_START.equals(tag)) {
            // 版本号
            bytes = new byte[1];
            randomAccessFile.read(bytes);
            // 副版本号
            bytes = new byte[1];
            randomAccessFile.read(bytes);
            // 标志，意义不大
            bytes = new byte[1];
            randomAccessFile.read(bytes);
            // 标签内容长度，高位在前，不包括标签头的 10 个字节
            bytes = new byte[4];
            randomAccessFile.read(bytes);
            // 标签内容长度，不包括标签头的 10 个字节，按照 ID3V2 标准 https://id3.org/id3v2.3.0#ID3v2_header 的要求，每个字节只用 7 位，
            // 最高位不使用，恒为 0，所以需要将 size 每个字节的最高位 0 位丢弃，而且它是高位在前。
            int size = bytes2Int(reverse(bytes));
            size = syncIntDecode(size);
            while (true) {
                // Frame Id，4 字节
                bytes = new byte[4];
                randomAccessFile.read(bytes);
                String frameId = new String(bytes);
                if (!frameId.matches("([A-Z]|[0-9]){4}")) {
                    break;
                }
                Frame frame = new Frame();
                frame.mId = frameId;
                // Frame size，4 字节
                bytes = new byte[4];
                randomAccessFile.read(bytes);
                int frameSize = bytes2Int(reverse(bytes));
                frame.mSize = frameSize;
                // Frame flag，2 字节，意义不大
                bytes = new byte[2];
                randomAccessFile.read(bytes);
                frame.mFlag = Arrays.copyOf(bytes, bytes.length);
                // Frame content
                bytes = new byte[frameSize];
                randomAccessFile.read(bytes);
                frame.mContent = Arrays.copyOf(bytes, bytes.length);
                frames.put(frameId, frame);
            }
            // 加上标签头的 10 个字节，srcRandomAccessFile seek 到 ID3V2 tag header 之后数据帧开始的位置，用于后面拷贝 mp3 数据帧
            randomAccessFile.seek(size + 10);
        }
        if (title != null) {
            // Title
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Text encoding.
            byteArrayOutputStream.write(0);
            // Title data.
            byteArrayOutputStream.write(title.getBytes(sCharset));
            frames.put("TIT2", new Frame("TIT2", byteArrayOutputStream.size(), new byte[2], byteArrayOutputStream.toByteArray()));
            byteArrayOutputStream.close();
        }
        if (artist != null) {
            // Artist
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Text encoding.
            byteArrayOutputStream.write(0);
            // Artist data.
            byteArrayOutputStream.write(artist.getBytes(sCharset));
            frames.put("TPE1", new Frame("TPE1", byteArrayOutputStream.size(), new byte[2], byteArrayOutputStream.toByteArray()));
            byteArrayOutputStream.close();
        }
        if (album != null) {
            // Album
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Text encoding.
            byteArrayOutputStream.write(0);
            // Album data.
            byteArrayOutputStream.write(album.getBytes(sCharset));
            frames.put("TALB", new Frame("TALB", byteArrayOutputStream.size(), new byte[2], byteArrayOutputStream.toByteArray()));
            byteArrayOutputStream.close();
        }
        /*
          Album Image
             <Header for 'Attached picture', ID: "APIC">
             Text encoding   $xx
             MIME type       <text string>
             $00
             Picture type    $xx
             Description     <text string according to encoding> $00 (00)
             Picture data    <binary data>
         */
        if (albumImg != null) {
            // Album Image
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Text encoding
            byteArrayOutputStream.write(0);
            // Mime type.
            byteArrayOutputStream.write("image/jpeg".getBytes(StandardCharsets.UTF_8));
            // 00
            byteArrayOutputStream.write(0);
            // Picture type
            byteArrayOutputStream.write(0);
            // Description
            byteArrayOutputStream.write(0);
            // Picture data
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(albumImg);
                byte[] buf = new byte[1024 * 8];
                int len = 0;
                while ((len = inputStream.read(buf)) != -1) {
                    byteArrayOutputStream.write(buf, 0, len);
                    byteArrayOutputStream.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            frames.put("APIC", new Frame("APIC", byteArrayOutputStream.size(), new byte[2], byteArrayOutputStream.toByteArray()));
            byteArrayOutputStream.close();
        }
        // Calculate ID3V2 size
        int id3V2Size = 0;
        for (Frame value : frames.values()) {
            // 每一个 Frame Header 为 10 字节
            id3V2Size += 10;
            id3V2Size += value.mSize;
        }
        // ID3V2 可以先预留一些空白标签帧，这样的好处是今后如果需要增加帧只需要覆盖空白字节即可
        // ，否则今后再想增加标签帧就需要又重写整个文件
//        byte[] empty = new byte[0];
        byte[] empty = new byte[100];
        id3V2Size += empty.length;
        // ID3V2 tag header
        ByteArrayOutputStream id3v2TagHeader = new ByteArrayOutputStream(10);
        // TAG 3 个字符开头，占 3 个字节
        id3v2TagHeader.write(ID3V2_TAG_START.getBytes());
        // 版本号
        id3v2TagHeader.write(3);
        // 副版本号
        id3v2TagHeader.write(0);
        // 标志，意义不大
        id3v2TagHeader.write(0);
        // 标签内容长度，不包括标签头的 10 个字节，按照 ID3V2 标准 https://id3.org/id3v2.3.0#ID3v2_header 的要求，每个字节只用 7 位，
        // 最高位不使用，恒为 0，所以需要将 size 每个字节的最高位 0 位丢弃，而且它是高位在前。
        int syncIntEncode = syncIntEncode(id3V2Size);
        byte[] reverse = reverse(int2Bytes(syncIntEncode));
        id3v2TagHeader.write(reverse);
        File dst = new File(src.getAbsolutePath() + ".tmp");
        FileOutputStream fileOutputStream = new FileOutputStream(dst);
        fileOutputStream.write(id3v2TagHeader.toByteArray());
        fileOutputStream.flush();
        for (Frame value : frames.values()) {
            // 每一个 Frame Header 为 10 字节
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10 + value.mSize);
            // Frame Id，4 字节
            byteArrayOutputStream.write(value.mId.getBytes());
            // Frame size，4 字节
            byteArrayOutputStream.write(reverse(int2Bytes(value.mSize)));
            // Frame flag，2 字节，意义不大
            byteArrayOutputStream.write(value.mFlag);
            // Frame content
            byteArrayOutputStream.write(value.mContent);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.flush();
        }
        fileOutputStream.write(empty);
        fileOutputStream.flush();
        bytes = new byte[1024 * 8];
        int len = 0;
        while ((len = randomAccessFile.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, len);
            fileOutputStream.flush();
        }
        randomAccessFile.close();
        src.delete();
        fileOutputStream.close();
        dst.renameTo(new File(src.getAbsolutePath()));
    }

    public static void removeID3V2(File src) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "r");
        randomAccessFile.seek(0);
        byte[] bytes = new byte[3];
        randomAccessFile.read(bytes);
        String tag = new String(bytes);
        if (!ID3V2_TAG_START.equals(tag)) {
            // 没有则直接 return
            randomAccessFile.close();
            return;
        }
        // 版本号
        bytes = new byte[1];
        randomAccessFile.read(bytes);
        // 副版本号
        bytes = new byte[1];
        randomAccessFile.read(bytes);
        // 标志，意义不大
        bytes = new byte[1];
        randomAccessFile.read(bytes);
        // 标签内容长度，高位在前，不包括标签头的 10 个字节
        bytes = new byte[4];
        randomAccessFile.read(bytes);
        // 标签内容长度，不包括标签头的 10 个字节，按照 ID3V2 标准 https://id3.org/id3v2.3.0#ID3v2_header 的要求，每个字节只用 7 位，
        // 最高位不使用，恒为 0，所以需要将 size 每个字节的最高位 0 位丢弃，而且它是高位在前。
        int size = bytes2Int(reverse(bytes));
        size = syncIntDecode(size);
        // 加上标签头的 10 个字节，srcRandomAccessFile seek 到 ID3V2 tag header 之后数据帧开始的位置，用于后面拷贝 mp3 数据帧
        randomAccessFile.seek(size + 10);
        File dst = new File(src.getAbsolutePath() + ".tmp");
        FileOutputStream fileOutputStream = new FileOutputStream(dst);
        bytes = new byte[1024 * 8];
        int len = 0;
        while ((len = randomAccessFile.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, len);
            fileOutputStream.flush();
        }
        randomAccessFile.close();
        src.delete();
        fileOutputStream.close();
        dst.renameTo(new File(src.getAbsolutePath()));
    }
}
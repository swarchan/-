import java.io.*;
import java.nio.charset.Charset;

/**
 * 参考：https://blog.csdn.net/zgcqflqinhao/article/details/129492621
 * 原理：使用IO流(RandomAccessFile)，修改ID3V1
 * 结论：文件属性没修改成功，QQ音乐也没修改成功
 */
public class WriteMp3InfoByLv1 {
    private static final Charset sCharset = Charset.forName("GBK");
    private static final int ID3V1_TAG_LENGTH = 128;
    private static final String ID3V1_TAG_START = "TAG";

    public static void main(String[] args) throws IOException {
        String fileName = "林俊杰-加油.mp3";
        String tmp = "";
        String album = "100天"+tmp;//所属专辑

        String newTitle = fileName.substring(0, fileName.lastIndexOf("."));
        String singerName = newTitle.split("-")[0]+tmp;
        String songName = newTitle.split("-")[1]+tmp;

        File src = new File("resources/"+fileName);
        removeID3V1(src);
        setID3V1(src, songName, singerName, album, null, null, null, null);
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

    public static void setID3V1(File src, String title, String artist, String album, Integer year, String comment, Byte track, Byte genre) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "rw");
        randomAccessFile.seek(randomAccessFile.length() - ID3V1_TAG_LENGTH);
        byte[] bytes = new byte[3];
        randomAccessFile.read(bytes);
        String tag = new String(bytes);
        if (ID3V1_TAG_START.equals(tag)) {
            // 以前有 ID3V1，则先获取之前的信息，再修改需要修改的
            // Title 占 30 字节
            bytes = new byte[30];
            randomAccessFile.read(bytes);
            if (title == null) {
                title = new String(bytes, sCharset);
            }
            // Artist 占 30 字节
            randomAccessFile.read(bytes);
            if (artist == null) {
                artist = new String(bytes, sCharset);
            }
            // Album 占 30 字节
            randomAccessFile.read(bytes);
            if (album == null) {
                album = new String(bytes, sCharset);
            }
            // Year 占 4 字节
            bytes = new byte[4];
            randomAccessFile.read(bytes);
            if (year == null) {
                year = bytes2Int(bytes);
            }
            // Comment 占 28 字节，没有曲目序号时占 30 字节
            bytes = new byte[30];
            randomAccessFile.read(bytes);
            // Reserved 占 1 字节，为 0 表示有曲目序号，下一字节为曲目序号
            if (bytes[28] == 0) {
                if (comment == null) {
                    comment = new String(bytes, 0, 28, sCharset);
                }
                if (track == null) {
                    track = bytes[29];
                }
            } else {
                if (comment == null) {
                    comment = new String(bytes, sCharset);
                }
            }
            // Genre 占 1 字节，歌曲风格，-1 表示没有风格
            bytes = new byte[1];
            randomAccessFile.read(bytes);
            if (genre == null) {
                genre = bytes[0];
            }
            randomAccessFile.seek(randomAccessFile.length() - ID3V1_TAG_LENGTH);
        } else {
            // 没有则直接定位到文件末尾追加
            randomAccessFile.seek(randomAccessFile.length());
        }
        // TAG 3 个字符开头，占 3 个字节
        bytes = ID3V1_TAG_START.getBytes();
        randomAccessFile.write(bytes);
        // Title 占 30 字节
        bytes = new byte[30];
        if (title != null) {
            byte[] tmp = title.getBytes(sCharset);
            System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 30));
        }
        randomAccessFile.write(bytes);
        // Artist 占 30 字节
        bytes = new byte[30];
        if (artist != null) {
            byte[] tmp = artist.getBytes(sCharset);
            System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 30));
        }
        randomAccessFile.write(bytes);
        // Album 占 30 字节
        bytes = new byte[30];
        if (album != null) {
            byte[] tmp = album.getBytes(sCharset);
            System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 30));
        }
        randomAccessFile.write(bytes);
        // Year 占 4 字节
        bytes = new byte[4];
        if (year != null) {
            byte[] tmp = int2Bytes(year);
            System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 4));
        }
        randomAccessFile.write(bytes);
        // Comment 占 28 字节，没有曲目序号时占 30 字节
        if (track == null) {
            // 没有曲目序号
            bytes = new byte[30];
            if (comment != null) {
                byte[] tmp = comment.getBytes(sCharset);
                System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 30));
            }
            randomAccessFile.write(bytes);
        } else {
            // 有曲目序号
            bytes = new byte[28];
            if (comment != null) {
                byte[] tmp = comment.getBytes(sCharset);
                System.arraycopy(tmp, 0, bytes, 0, Math.min(tmp.length, 28));
            }
            randomAccessFile.write(bytes);
            // Reserved 占 1 字节，为 0 表示有曲目序号，下一字节为曲目序号
            bytes = new byte[]{0};
            randomAccessFile.write(bytes);
            // Track 占 1 字节，曲目序号
            bytes = new byte[]{track};
            randomAccessFile.write(bytes);
        }
        // Genre 占 1 字节，歌曲风格，-1 表示没有风格
        bytes = new byte[1];
        if (genre == null) {
            bytes[0] = -1;
        } else {
            bytes[0] = genre;
        }
        randomAccessFile.write(bytes);
        randomAccessFile.close();
    }

    public static void removeID3V1(File src) throws IOException {
        System.out.println("Remove ID3V1 start.");
        RandomAccessFile randomAccessFile = new RandomAccessFile(src, "rw");
        randomAccessFile.seek(randomAccessFile.length() - ID3V1_TAG_LENGTH);
        byte[] bytes = new byte[3];
        randomAccessFile.read(bytes);
        String tag = new String(bytes);
        if (!ID3V1_TAG_START.equals(tag)) {
            randomAccessFile.close();
            System.out.println("Remove ID3V1 end.");
            return;
        }
        randomAccessFile.setLength(randomAccessFile.length() - ID3V1_TAG_LENGTH);
        System.out.println("Remove ID3V1 end.");
    }

}

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.*;

/**
 * 参考：https://www.freesion.com/article/4774466646/
 * 原理：使用IO流(RandomAccessFile)，读取前面的ID3V1
 * 结论：读取文件流
 */
public class ReadMp3Info {
    public static void main(String args[]) {
        try {
            RandomAccessFile raf = new RandomAccessFile("resources2/林俊杰-加油.mp3", "rw");
            byte buf[] = new byte[128];
            byte buf1[] = new byte[4];
            raf.seek(raf.length() - 128);
            raf.read(buf);
            raf.seek(45);// 指针往前走45个字节，从第46个字节开始
            raf.read(buf1);
            if (buf.length != 128) {
                System.err.println("MP3标签信息数据长度不合法!");
            }
            if (!"TAG".equalsIgnoreCase(new String(buf, 0, 3))) {
                System.err.println("MP3标签信息数据格式不正确!");
            }
            String SongName = new String(buf, 3, 30,"gbk");
            System.out.println("歌名：" + SongName);
            String Singer = new String(buf, 33, 30,"gbk");
            System.out.println("作者：" + Singer);
            String Album = new String(buf, 63, 30,"gbk");
            System.out.println("专辑：" + Album);
            System.out.println("文件长度：" + raf.length());
            raf.close();
            // 将字节转化为二进制字符串
            String s1 = String.format("%8s", Integer.toBinaryString(buf1[0] & 0xFF)).replace(' ', '0');
            String s2 = String.format("%8s", Integer.toBinaryString(buf1[1] & 0xFF)).replace(' ', '0');
            String s3 = String.format("%8s", Integer.toBinaryString(buf1[2] & 0xFF)).replace(' ', '0');
            String s4 = String.format("%8s", Integer.toBinaryString(buf1[3] & 0xFF)).replace(' ', '0');
            String c = s1 + s2 + s3 + s4;
            char sc[] = c.toCharArray();
            System.out.println("音频版本：" + AudioVersion(sc[11], sc[12]));
            System.out.println("采样频率：" + SampleFrequency(sc[11], sc[12], sc[20], sc[21]));
            System.out.println("声道模式：" + ChannelMode(sc[24], sc[25]));

        } catch (IOException e) {
            System.err.println("发生异常:" + e);
            e.printStackTrace();
        }
    }
    //判断音频版本
    public static String AudioVersion(char a, char b) {
        String result = "";
        if ('0' == a && '0' == b)
            result = "MPEG 2.5";
        else if ('0' == a && '1' == b)
            result = "保留";
        else if ('1' == a && '0' == b)
            result = "MPEG 2";
        else if ('1' == a && '1' == b)
            result = "MPEG 1";
        return result;
    }
    //判断采样频率
    public static int SampleFrequency(char a1, char b1, char a, char b) {
        int f = 0;
        if (AudioVersion(a1, b1) == "MPEG 1" && '0' == a && '0' == b)
            f = 44100;
        if (AudioVersion(a1, b1) == "MPEG 2" && '0' == a && '0' == b)
            f = 22050;
        if (AudioVersion(a1, b1) == "MPEG 2.5" && '0' == a && '0' == b)
            f = 11025;
        if (AudioVersion(a1, b1) == "MPEG 1" && '0' == a && '1' == b)
            f = 48000;
        if (AudioVersion(a1, b1) == "MPEG 2" && '0' == a && '1' == b)
            f = 24000;
        if (AudioVersion(a1, b1) == "MPEG 2.5" && '0' == a && '1' == b)
            f = 12000;
        if (AudioVersion(a1, b1) == "MPEG 1" && '1' == a && '0' == b)
            f = 32000;
        if (AudioVersion(a1, b1) == "MPEG 2" && '1' == a && '0' == b)
            f = 16000;
        if (AudioVersion(a1, b1) == "MPEG 2.5" && '1' == a && '0' == b)
            f = 8000;
        return f;
    }
    //判断声道模式
    public static String ChannelMode(char a, char b) {
        String result = "";
        if ('0' == a && '0' == b)
            result = "立体声";
        else if ('0' == a && '1' == b)
            result = "联合立体声";
        else if ('1' == a && '0' == b)
            result = "双声道";
        else if ('1' == a && '1' == b)
            result = "单声道";
        return result;
    }
}
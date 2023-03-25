import org.jaudiotagger.audio.mp3.MP3AudioHeader;

import org.jaudiotagger.audio.mp3.MP3File;

import org.jaudiotagger.tag.Tag;

import org.jaudiotagger.tag.id3.AbstractID3Tag;

import org.jaudiotagger.tag.id3.AbstractID3v1Tag;

import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

import org.jaudiotagger.tag.id3.ID3v1Tag;

import org.jaudiotagger.tag.id3.ID3v24Tag;

import org.jaudiotagger.tag.images.Artwork;


/**
 * 参考：https://blog.csdn.net/weixin_30129661/article/details/114450716
 * 原理：使用jaudiotagger-2.2.2.jar，读取ID3V2
 * 结论：读取文件属性值，与QQ音乐读取不一样
 */
public class ReadMp3InfoByJauDio {

    public static void main(String[] args) {

        getHead();

        getContent();

    }


    private static MP3File mp3File;

    private static final int START=6;

    private static void getHead() {

        String fileName = "林俊杰-加油.mp3";
        String mp3Path = "resources1/"+fileName;
        try {

            System.out.println("----------------Loading...Head-----------------");

            mp3File = new MP3File(mp3Path);//封装好的类

            MP3AudioHeader header = mp3File.getMP3AudioHeader();

            System.out.println("时长: " + header.getTrackLength()); //获得时长

            System.out.println("比特率: " + header.getBitRate()); //获得比特率

            System.out.println("音轨长度: " + header.getTrackLength()); //音轨长度

            System.out.println("格式: " + header.getFormat()); //格式，例 MPEG-1

            System.out.println("声道: " + header.getChannels()); //声道

            System.out.println("采样率: " + header.getSampleRate()); //采样率

            System.out.println("MPEG: " + header.getMpegLayer()); //MPEG

            System.out.println("MP3起始字节: " + header.getMp3StartByte()); //MP3起始字节

            System.out.println("精确的音轨长度: " + header.getPreciseTrackLength()); //精确的音轨长度

        } catch (Exception e) {

            System.out.println("没有获取到任何信息");

        }

    }

    private static void getContent() {

        try {

            System.out.println("----------------Loading...Content-----------------");

            AbstractID3v2Tag id3v2tag=  mp3File.getID3v2Tag();

            String songName=new String(id3v2tag.frameMap.get("TIT2").toString().getBytes("ISO-8859-1"),"GB2312");

            String singer=new String(id3v2tag.frameMap.get("TPE1").toString().getBytes("ISO-8859-1"),"GB2312");

            String author=new String(id3v2tag.frameMap.get("TALB").toString().getBytes("ISO-8859-1"),"GB2312");

            System.out.println("歌名："+songName.substring(START, songName.length()-3));

            System.out.println("歌手:"+singer.substring(START,singer.length()-3));

            System.out.println("专辑名："+author.substring(START,author.length()-3));

        } catch (Exception e) {

            System.out.println("没有获取到任何信息");

        }

        System.out.println("All Info："+mp3File.displayStructureAsPlainText());

    }

}
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 修改MP3文件名
 */
public class mp3 {

    static final String dealSuffix[] = "mp3,wma,lrc".split(",");
    static Set<String> singerList = new HashSet<>();
    static Set<String> dirMap = new HashSet<>();

    public static void main(String[] args) throws Exception {
        System.out.println("请输入操作内容：\n" +
                "\t\"1\"：格式化歌手\n" +
                "\t其他：格式化歌名");

        BufferedReader buf=new BufferedReader(new InputStreamReader(System.in));
        String op = buf.readLine();

        if("1".equals(op)){
            beautySinger();
            System.out.println();
            System.out.println("处理完成，生成文件：resources/歌手集合.txt");
        }else{
            dealSong();
            System.out.println("处理完成，已替换歌名");
        }
    }

    public static void beautySinger() throws Exception{
        String tmpSinger = "resources/tmp-歌手集合.txt";

        File file = new File(tmpSinger);
        if(!file.exists()){
            System.out.println("请填写文件：resources/tmp-歌手集合.txt（使用逗号，句号，或换行分隔）");
            System.out.println("程序已退出");
            System.exit(0);
        }

        //new FileReader(tmpSinger)=默认GBK
        Set<String> singer = new HashSet<>();
        try (Scanner sc = new Scanner(new InputStreamReader(new FileInputStream(tmpSinger),StandardCharsets.UTF_8))) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if("".equals(line)) continue;
                //增加每一行
                singer.add(line);
                //数据切分
                singer = splitSet(singer, ",");
                singer = splitSet(singer, "，");
                singer = splitSet(singer, "、");
                singer = splitSet(singer, "。");
                singer = splitSet(singer, "\\.");
            }
        }

        //set做不了排序（排完后，它默认会打乱），只能用数组排序
        String[] singerList = singer.toArray(new String[singer.size()]);
        //按拼音排序
        Arrays.sort(singerList, Collator.getInstance(Locale.CHINA));

        //保存文件
        String targetSinger = "resources/歌手集合2.txt";
        writeToFileAppend(targetSinger, singerList);

    }

    /**
     * 写入文件（替换文件）
     */
    public static void writeToFile(String fileName,String[] text) throws Exception {
        File file = new File(fileName);
        FileWriter fr = null;
        try {
            fr = new FileWriter(file);
            for (String s : text) {
                fr.write(s);
                fr.write("\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 写入文件（追加）
     */
    public static void writeToFileAppend(String fileName, String[] text) {
        OutputStreamWriter fw2 = null;
        try {
            //如果文件存在，则追加内容；如果文件不存在，则创建文件
            File f = new File(fileName);
            //new FileWriter(f, true)=默认GBK
            fw2 = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(fw2);
        for (String s : text) {
            pw.println(s);
        }
        pw.flush();
        try {
            fw2.flush();
            pw.close();
            fw2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Set<String> splitSet(Set<String> data, String separator){
        Set<String> result = new HashSet<>();
        data.forEach(t->{
            if("".equals(t)) return;
            //Collections.addAll(result, t.split(separator)));
            result.addAll(Arrays.stream(t.split(separator)).map(m->m.trim().toUpperCase()).collect(Collectors.toSet()));
        });
        return result;
    }

    /*************************************************************************
     * * * * * * * * * * * 以下是处理文件 * * * * * * * * * * * *
     *************************************************************************/

    public static void dealSong() throws Exception {
        String singerFile = "resources/歌手集合.txt";
        String dirFile = "resources/处理文件夹.txt";


        File file = new File(singerFile);
        if(!file.exists()){
            System.out.println("请填写文件：歌手集合.txt(使用英文逗号或换行分隔)");
            System.out.println("程序已退出");
            System.exit(0);
        }


        file = new File(dirFile);
        if(!file.exists()){
            System.out.println("请填写文件：处理文件夹.txt(使用英文逗号或换行分隔)");
            System.out.println("程序已退出");
            System.exit(0);
        }

        try (Scanner sc = new Scanner(new InputStreamReader(new FileInputStream(singerFile),StandardCharsets.UTF_8))) {
            while (sc.hasNextLine()) {
                //处理输入了逗号
                String line = sc.nextLine().replace(",","").replace("，","");
                singerList.add(line);
            }
        }

        try (Scanner sc = new Scanner(new InputStreamReader(new FileInputStream(dirFile),StandardCharsets.UTF_8))) {
            while (sc.hasNextLine()) {  //按行读取字符串
                String line = sc.nextLine().replace(",","").replace("，","").trim();
                System.out.println(line);
                //盘符前，隐藏了占位符，:前一位为盘符
                line = line.substring(line.indexOf(":")-1);
                dirMap = getFolderList(line);
            }
        }

        for (String dir : dirMap) {
            reNameMp3(dir);
        }

    }

    /**
     * 获取根目录下的所有文件夹路径
     */
    public static Set<String> getFolderList(String root){
        File file = new File(root);
        if (!file.exists()) {
            System.out.println(root);
            System.out.println("读取文件夹有误，中文问题？");
            return null;
        }
        Set<String> allFolder=new HashSet<>();
        findFolder(file,allFolder);
        return allFolder;
    }

    /**
     * 递归获取
     */
    private static void findFolder(File file,Set<String> allFolder){
        //剔除编译路径，文件目录
        if(file.getAbsolutePath().indexOf("out") > -1) return;

        if(file.isDirectory()){
            allFolder.add(file.getAbsolutePath());
            File[] files= file.listFiles();
            for(File f:files){
                findFolder(f,allFolder);
            }
        }
    }

    //getAbsolutePath   文件全路径
    //getParent   不包括文件名
    //getName   文件名+后缀
    public static void reNameMp3(String dir){
        System.out.println("正在处理文件夹：" + dir);

        File file = new File(dir);
        if(file.isDirectory()){
            File[] files = file.listFiles();
            for(int i=0; i<files.length; i++){
                //只处理文件
                if(files[i].isDirectory()) continue;
                //只处理有后缀的文件
                if(files[i].getName().indexOf(".")==-1) continue;

                String oldName = files[i].getName();
                String suffix = oldName.substring(oldName.lastIndexOf("."));

                //只处理特定格式
                if(Arrays.stream(dealSuffix).allMatch(s->suffix.toLowerCase().indexOf(s.toLowerCase())==-1)) continue;

                //歌手
                Set<String> matchSingerList = singerList.stream()
                        .filter(n->oldName.replace(".","").toLowerCase().indexOf(n.toLowerCase())>-1)
                        .collect(Collectors.toSet());

                //歌名
                String songName = oldName.replace("-","").replace(suffix,"")
                        //替换123.
                        .replaceAll("[\\d]+\\.","")
                        .replace("《","")
                        .replace("》","")
                        .replace("+","")
                        .replace(".","").replace("。","")
                        .replace(",","").replace("，","").trim();

                for (String matchSinger : matchSingerList) {
                    songName = songName.toLowerCase().replace(matchSinger.toLowerCase(),"");
                }

                //歌手名 + "-" + 歌名 + 后缀
                String newName;
                if(matchSingerList.size() == 0){
                    newName = songName + suffix;
                }else{
                    //用+号隔开多个歌手
                    newName = String.join("+", matchSingerList) + "-" + songName + suffix;
                }

                System.out.println("修改前：" + oldName);
                String newNamePath = files[i].getParent()
                        + "\\" + newName;
                System.out.println("修改后：" + newNamePath);
				//修改文件属性中的，歌手+歌名
                boolean isModifyProp = Arrays.stream("mp3,wma".split(","))
                        .anyMatch(s->suffix.toLowerCase().indexOf(s.toLowerCase())!=-1);
                if(true){
                    try{
                        WriteMp3InfoByLv2.setID3V2(files[i],songName,String.join("+", matchSingerList),
                                null,
                                null);
                    }catch (Exception e){
                        System.out.println("修改文件属性失败："+songName);
                        e.printStackTrace();
                    }
                }
                //修改文件名
                files[i].renameTo(new File(newNamePath));
            }
        }
        System.out.println();
    }

}

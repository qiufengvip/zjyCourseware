package com.qiufeng;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * 职教云类
 * 秋枫：2020.6.14
 * 秋枫: 2020.6.18  修复过期课件引起的异常（1.1）
 * 秋枫：2020.6.22  增加笔记栏目，增加单刷评论无法刷，修复纠错、问答、开启判断错误（1.2）
 * 秋枫：2020.7.4   修复登录问题(1.3)
 * 秋枫：2020.7.29  修复登录失效(1.4)
 * 秋枫：2020.8.28  修复版本过期问题，自动获取最新版本(1.5)
 * 秋枫：2020.10.1  修改参数 业务逻辑改变 更安全(1.6)
 * 秋枫：2021.3.26  修复刷课获取不到视频时长问题，部分课件发送错误可以智能跳过单个(1.6)
 * 版本：1.6
 */
public class ZjyCourseware extends Thread{

    public String message="";
    private String cookie;
    private String courseOpenId;
    private String openClassId;
    private String newToken;
    private String userId;
    private String token;  //职教管家的token  用来获取职教云的用户登录信息

    private boolean iscomment;   //开启评论
    private boolean iserror;     //开启纠错
    private boolean isissue;     //问答
    private boolean isCellNote;  //笔记
    private boolean islesson;    //是否刷课




    /**
     * 这行删掉---------------------------
     */
    系统相关类  系统相关类= new 系统相关类();

    public  void 初始化(String token,String courseOpenId, String openClassId,boolean iscomment,boolean iserror,boolean isissue,boolean isCellNote,boolean islesson){
        this.token = token;
        this.courseOpenId=courseOpenId;
        this.openClassId=openClassId;
        this.iscomment=iscomment;
        this.iserror = iserror;
        this.isissue = isissue;
        this.isCellNote = isCellNote;
        this.islesson = islesson;  //是否刷课
    }

    @Override
    public void run() {
        //单元->章节->节->小节
//        this.message+="\nGet cookie sucess，mission start...";
        系统相关类.发送广播("test",1,"Get cookie sucess，mission start...");
        if (getcookie()){
            系统相关类.发送广播("test",1,"Get cookie 成功");
        }else{
            系统相关类.发送广播("test",1,"Get cookie 失败 线程退出");
            return;
        }

        //获取单元信息
        String infounit=this.getunit();
        System.out.println(infounit);
        JSONObject json_info_unit = null;
        try {
            json_info_unit = new JSONObject(infounit);//解析成json对象
            JSONObject json_info_unit_progress = json_info_unit.getJSONObject("progress");//解析对象下面的
            System.out.println(json_info_unit_progress);
            //每个单元解析数组
            JSONArray son_info_unit_moduleList =json_info_unit_progress.getJSONArray("moduleList");
            for (int i = 0; i < son_info_unit_moduleList.length(); i++) { //单元总循环---------------------
                getcookie();//更新cookie
                JSONObject json_unit=son_info_unit_moduleList.getJSONObject(i);//获取每一个单元的json信息
                int info_unit_single = json_unit.getInt("percent");//获取单元的进度
//                this.message+="\n正在执行单元名称->"+json_unit.getString("name")+"\t进度："+info_unit_single+"%";
                系统相关类.发送广播("test",1,"正在执行单元名称->"+json_unit.getString("name")+"\t进度："+info_unit_single+"%");
                if (this.islesson && info_unit_single==100){
//                    this.message+="\n本单元的学习进度已满，智能跳过。";
                    系统相关类.发送广播("test",1,"本单元的学习进度已满，智能跳过。");
                    continue;
                }
                String moduleId =json_unit.getString("id");
                String info_section=this.getsection(moduleId); // 返回章节信息
                System.out.println(info_section);
                JSONObject json_info_section=new JSONObject(info_section);//章节信息转JSON
                JSONArray json_info_section_topicList =json_info_section.getJSONArray("topicList");//解析每一章节成为数组
                for (int i1=0;i1 < json_info_section_topicList.length();i1++){//遍历单元变为章
                    JSONObject json_section = json_info_section_topicList.getJSONObject(i1);
//                    this.message+="\n正在处理章节->"+json_section.getString("name");
                    系统相关类.发送广播("test",1,"正在处理章节->"+json_section.getString("name"));
                    String sectionid = json_section.getString("id");
                    String info_classinfo = this.getclassinfo(sectionid);
                    JSONObject json_info_classinfo = new JSONObject(info_classinfo);
                    JSONArray json_info_cellList = json_info_classinfo.getJSONArray("cellList");//解析每一节成为数组
                    for (int i2=0; i2< json_info_cellList.length();i2++){//遍历章变为节
                        JSONObject json_info_cellId = json_info_cellList.getJSONObject(i2); //解析小节为json对象
//                        this.message+="\n正在处理小节->"+json_info_cellId.getString("cellName");
                        系统相关类.发送广播("test",1,"正在处理小节->"+json_info_cellId.getString("cellName"));
                        String cellId = json_info_cellId.getString("Id");//节id
                        String categoryName = null;
                        try {
                            categoryName = json_info_cellId.getString("categoryName");
                            switch (categoryName){

                                case "视频":
                                case "音频":

                                    int stuCellPercent = json_info_cellId.getInt("stuCellPercent");//进度信息
//                                this.message+="\n开始处理视频->时间稍长请耐心等待..." +"\t进度：" + stuCellPercent+"%";
                                    系统相关类.发送广播("test",1,"开始处理视频->时间稍长请耐心等待..." +"  进度：" + stuCellPercent+"%");
                                    if (this.islesson && stuCellPercent==100){
//                                    this.message+="\n本节已学习完毕,跳过！";
                                        系统相关类.发送广播("test",1,"本节已学习完毕,跳过！");
                                        continue;
                                    }else {

                                        //String moduleId,String cellId,String cellName
                                        this.statement(moduleId,cellId,json_info_cellId.getString("cellName"));//声明操作域
//                                    String info_video = this.getvideo(cellId,moduleId);//获取视频信息
                                        JSONObject json_info_video = this.getvideo(cellId,moduleId);//json对象-视频信息
                                        /**
                                         * 秋枫
                                         * 6.18
                                         * 修复异常
                                         */
                                        try {
                                            //-33证明 单元没开启
                                            if (json_info_video.getInt("code")==-33){
                                                continue;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        this.comment(cellId);//添加评价等内容
                                        if (!this.islesson){
                                            try {
                                                Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            continue;
                                        }

                                        int info_video_code = json_info_video.getInt("code");
                                        String info_token =json_info_video.getString("guIdToken"); //token
                                        String info_videoTimeTotalLong =json_info_video.getDouble("audioVideoLong")+"";//总长度
                                        int stuStudyNewlyTime = json_info_video.getInt("stuStudyNewlyTime"); //以看长度

                                        if (info_video_code==1){
                                            this.videodispose(cellId,info_token,info_videoTimeTotalLong,stuStudyNewlyTime);
//                                        this.videodisposebuff(cellId,info_token,info_videoTimeTotalLong);
                                        }else {
//                                        this.message+="\n解析详细信息异常。";
                                            系统相关类.发送广播("test",1,"解析详细信息异常。");
                                        }
                                    }
                                    break;
                                case "ppt":
                                case "文档":
                                case "链接":
                                case "图文":
                                case "图片":
                                case "压缩包":
                                case "swf":
                                case "其他":
                                    int pptstuCellPercent = json_info_cellId.getInt("stuCellPercent");//进度信息
//                                this.message+="\n开始处理文档->时间稍长请耐心等待..." +"\t进度：" + pptstuCellPercent+"%";
                                    系统相关类.发送广播("test",1,"开始处理文档->时间稍长请耐心等待..." +"  进度：" + pptstuCellPercent+"%");
                                    if (this.islesson && pptstuCellPercent==100){
//                                    this.message+="\n本节已学习完毕,跳过!";
                                        系统相关类.发送广播("test",1,"本节已学习完毕,跳过!");
                                        continue;
                                    }else {

                                        this.statement(moduleId,cellId,json_info_cellId.getString("cellName"));
//                                    String info_ppt=this.getppt(cellId,moduleId);//获取文档详细信息
//                                    System.out.println(info_ppt);

                                        JSONObject json_info_ppt = this.getppt(cellId,moduleId);//文档详细信息-json对象
                                        /**
                                         * 秋枫
                                         * 6.18
                                         * 修复异常
                                         */
                                        try {
                                            if (json_info_ppt.getInt("code")==-33){
                                                continue;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        this.comment(cellId);
                                        if (!this.islesson){
                                            try {
                                                Thread.sleep(10000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            continue;
                                        }


                                        int ppt_code = json_info_ppt.getInt("code");//响应码
                                        String ppt_token =json_info_ppt.getString("guIdToken");//token
                                        String ppt_pageCount = json_info_ppt.getInt("pageCount")+"";//
                                        this.pptdispose(cellId,ppt_token,ppt_pageCount);
//                                    this.videodisposebuff(cellId,ppt_token,ppt_pageCount);
                                    }

                                    break;


                                case "子节点":
//                                this.message+="\n正在处理子节点："+json_info_cellId.getString("cellName");
                                    系统相关类.发送广播("test",1,"正在处理子节点："+json_info_cellId.getString("cellName"));
                                    JSONArray childNodeList = json_info_cellId.getJSONArray("childNodeList");
                                    for (int i_seed=0;i_seed<childNodeList.length();i_seed++){
                                        JSONObject json_seed_info_cellId = childNodeList.getJSONObject(i_seed);//一节的id
                                        String seed_cellId = json_seed_info_cellId.getString("Id");//小节id
                                        String seed_cellName = json_seed_info_cellId.getString("cellName");//小节名称
//                                    this.message+="\n正在处理子节点-小节："+seed_cellName;
                                        系统相关类.发送广播("test",1,"正在处理子节点-小节："+seed_cellName);
                                        String seed_categoryName = json_seed_info_cellId.getString("categoryName");//类型
                                        switch (seed_categoryName){
                                            case "视频":
                                            case "音频":
                                                int seed_stuCellFourPercent = json_seed_info_cellId.getInt("stuCellFourPercent");//进度信息
//                                            this.message+="\t进度："+seed_stuCellFourPercent+"%";
                                                系统相关类.发送广播("test",1,"进度："+seed_stuCellFourPercent+"%");
                                                if (this.islesson && seed_stuCellFourPercent==100){
                                                    this.message+="\n本子节点中的本小节已学习完毕,跳过!";
                                                    系统相关类.发送广播("test",1,"本子节点中的本小节已学习完毕,跳过!");
                                                    continue;
                                                }else {
                                                    this.statement(moduleId,seed_cellId,seed_cellName);//声明操作域
//                                                String seed_info_video=this.getvideo(seed_cellId,moduleId);
                                                    JSONObject json_seed_info_video = this.getvideo(seed_cellId,moduleId);//视频信息
                                                    System.out.println("\n***************"+json_seed_info_video+"\n***************");
                                                    /**
                                                     * 秋枫
                                                     * 6.18
                                                     * 修复异常
                                                     */
                                                    try {
                                                        if (json_seed_info_video.getInt("code")==-33){
                                                            continue;
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    this.comment(seed_cellId);
                                                    if (!this.islesson){
                                                        try {
                                                            Thread.sleep(10000);
                                                        } catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                        }
                                                        continue;
                                                    }



                                                    int seed_info_video_code = json_seed_info_video.getInt("code");
                                                    String seed_info_token =json_seed_info_video.getString("guIdToken"); //token
//                                            String seed_info_cellId =json_seed_info_video.getString("cellId"); //ceelid
                                                    String seed_info_videoTimeTotalLong =json_seed_info_video.getDouble("audioVideoLong")+"";//总长度
                                                    int seed_stuStudyNewlyTime = json_seed_info_video.getInt("stuStudyNewlyTime"); //以看长度

                                                    if (seed_info_video_code==1){
                                                        System.out.println(seed_cellId);
                                                        System.out.println(seed_info_token);
                                                        System.out.println(seed_info_videoTimeTotalLong);
//                                                    System.out.println(seed_stuStudyNewlyTime);
                                                        this.videodispose(seed_cellId,seed_info_token,seed_info_videoTimeTotalLong,seed_stuStudyNewlyTime);
//                                                    this.videodisposebuff(seed_cellId,seed_info_token,seed_info_videoTimeTotalLong);
                                                    }else {
//                                                    this.message+="\n解析子节点视频详细信息异常。";
                                                        系统相关类.发送广播("test",1,"解析子节点视频详细信息异常。");
                                                    }
                                                }
                                                break;
                                            case "ppt":
                                            case "文档":
                                            case "链接":
                                            case "图文":
                                            case "图片":
                                            case "压缩包":
                                            case "swf":
                                            case "测验":
                                            case "其他":
                                                int seed_ppt_stuCellFourPercent = json_seed_info_cellId.getInt("stuCellFourPercent");//进度信息
                                                if (this.islesson && seed_ppt_stuCellFourPercent==100){
//                                                this.message+="\n本子节点中的本小节已学习完毕,跳过!";
                                                    系统相关类.发送广播("test",1,"本子节点中的本小节已学习完毕,跳过!");
                                                    continue;
                                                }else {

                                                    this.statement(moduleId,seed_cellId,seed_cellName);//声明操作域
//                                                String seed_info_ppt = this.getppt(seed_cellId,moduleId);
                                                    JSONObject json_seed_info_ppt = this.getppt(seed_cellId,moduleId);
                                                    /**
                                                     * 秋枫
                                                     * 6.18
                                                     * 修复异常
                                                     */
                                                    try {
                                                        if (json_seed_info_ppt.getInt("code")==-33){
                                                            continue;
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    this.comment(seed_cellId);
                                                    if (!this.islesson){
                                                        try {
                                                            Thread.sleep(10000);
                                                        } catch (InterruptedException e) {
                                                            e.printStackTrace();
                                                        }
                                                        continue;
                                                    }


                                                    int seed_ppt_code = json_seed_info_ppt.getInt("code");
                                                    String seed_token = json_seed_info_ppt.getString("guIdToken");
                                                    //String seed_cellId =json_seed_info_ppt.getString("cellId"); //ceelid
                                                    String seed_pageCount = json_seed_info_ppt.getInt("pageCount")+"";
                                                    this.pptdispose(seed_cellId,seed_token,seed_pageCount);
//                                                this.videodisposebuff(seed_cellId,seed_token,seed_pageCount);
                                                }
                                                break;
                                        }
                                    }
                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("扎在这里");
                        }




                    }
                }
            }




            系统相关类.发送广播("test",1,"【完成】本科目已学习完毕");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }






//    public zhijiaoyun(String user, String wd,String courseOpenId,String openClassId) {
//        String login_info = this.login(user, wd);
//        this.logininfo=login_info;
//        this.courseOpenId=courseOpenId;
//        this.openClassId=openClassId;
//    }



    public String 获取信息() {
        return message;
    }











    public String getunit(){
        /**
         * 获取单元信息
         * cookies：
         * json:data
         */
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        zHttpRequest getunitdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/study/process/getProcessList", "POST", headers,this.cookie, data);
            return getunitdata.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return "获取单元信息失败";
        }
    }

    public String getsection(String moduleId){
        /**
         * 获取章节id
         * cookie
         * courseOpenId：课程ID
         * moduleId：单元ID
         * return: json data
         */

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("moduleId", moduleId);
        zHttpRequest getunitdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/study/process/getTopicByModuleId", "POST",headers,this.cookie, data);
            return getunitdata.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return "获取章节操作异常";
        }
    }

    public String getclassinfo(String topicId){
        /**
         * 获取小节操作
         * courseOpenId
         * openClassId
         * topicId：小节id
         */

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("topicId",topicId);
        zHttpRequest getunitdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/study/process/getCellByTopicId", "POST",headers,this.cookie, data);
            return getunitdata.getData();
        } catch (Exception e) {
            e.printStackTrace();
            return "获取小节操作异常";
        }


    }

    public JSONObject getvideo(String cellId,String moduleId){
        /**
         * 获取视频信息
         */
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("flag","s");
        data.put("moduleId",moduleId);
        zHttpRequest getunitdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/viewDirectory", "POST",headers,this.cookie, data);
            JSONObject JSONdata = new JSONObject(getunitdata.getData());
            JSONObject JSONresUrl = new JSONObject(JSONdata.getString("resUrl"));
            String statusURL = JSONresUrl.getJSONObject("urls").getString("status");
            String mp3URL = JSONresUrl.getJSONObject("urls").getString("preview");
            zHttpRequest getvideoinfo = new zHttpRequest(statusURL,"GET");
            JSONObject JSONvideoingo = new JSONObject(getvideoinfo.getData());
            String videologo = JSONvideoingo.getJSONObject("args").getString("duration");
            System.out.println(videologo);
            String[]  videoinfos=videologo.split("\\.");
            int duration = getSecond(videoinfos[0]);
            if(duration ==0){
                duration = getAudioPlayTime(mp3URL);
            }
            System.out.println("获取视频长度为"+duration);
            JSONdata.put("audioVideoLong",duration);


            return JSONdata;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @desc 转秒数 格式为 00:00:00
     * @param time
     * @return
     */
    public static int getSecond(String time){
        int s = 0;
        if(time.length()==8){ //时分秒格式00:00:00
            int index1=time.indexOf(":");
            int index2=time.indexOf(":",index1+1);
            s = Integer.parseInt(time.substring(0,index1))*3600;//小时
            s+=Integer.parseInt(time.substring(index1+1,index2))*60;//分钟
            s+=Integer.parseInt(time.substring(index2+1));//秒
        }
        if(time.length()==5){//分秒格式00:00
            s = Integer.parseInt(time.substring(time.length()-2)); //秒  后两位肯定是秒
            s+=Integer.parseInt(time.substring(0,2))*60;    //分钟
        }
        return s;
    }

    /**
     * @desc 获取音频时长
     * @param url
     * @return
     */
    public static int getAudioPlayTime(String url){
        try {
            long startTime=System.currentTimeMillis();   //获取开始时间
            URL urlfile = new URL(url);
            //File file = new File("C:\\music\\test2.mp3");
            //URL urlfile = file.toURI().toURL();
            URLConnection con = urlfile.openConnection();
            int b = con.getContentLength();// 得到音乐文件的总长度
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            Bitstream bt = new Bitstream(bis);
            Header h = bt.readFrame();
            int time = (int) h.total_ms(b);
            time=time / 1000;
            long endTime1=System.currentTimeMillis(); //获取结束时间
            System.out.println("所需时间： "+(endTime1-startTime)+"ms");
            return time;
        }catch (Exception e ){
            System.out.println(e.getMessage());
        }
        return 50;

    }






    /**
     * @desc 没用到
     * @param cellId
     * @param token
     */
    public  void getvideodisposeinfo(String cellId,String token){
        /**
         * 获取视频真实信息
         * courseOpenId
         * openClassId
         * cellId
         * token
         */
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("cellLogId","");
        data.put("picNum","0");
        data.put("studyNewlyTime","0");
        data.put("studyNewlyPicNum","0");
        data.put("token",token);


        try {
            new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId","5.5210");
        data.put("cellLogId","");
        data.put("picNum","0");
        data.put("studyNewlyTime","0");
        data.put("studyNewlyPicNum","0");
        data.put("token",token);


        try {
            new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);

        } catch (Exception e) {
            e.printStackTrace();
        }



    }




    /**
     * 处理视频和 ppt 2.0  炸了
     * @param cellId      本节课的id
     * @param token       本节课的操作token
     * @param videoTimeTotalLong    总长度
     */
    public void videodisposebuff(String cellId,String token,String videoTimeTotalLong){

//        var successCount = 0;
//        var failedCount = 0;
//        var totalCount = 0;

        double randomRequestTimes = (Math.random() * 87 + 56);
//        const requestData = {
//            courseOpenId: data.courseOpenId,
//            openClassId: data.openClassId,
//            cellId: data.cellId,
//            cellLogId: data.cellLogId,
//            picNum: Math.round(324 / randomRequestTimes),
//            studyNewlyTime: Math.round(14640 / randomRequestTimes),
//            studyNewlyPicNum: Math.round(324 / randomRequestTimes),
//            token: data.guIdToken
//        }

        double picNum =  Math.round(324 / randomRequestTimes);
        double studyNewlyTime=Math.round(14640 / randomRequestTimes);
        double studyNewlyPicNum=Math.round(324 / randomRequestTimes);


        System.out.println("长度="+randomRequestTimes);
        System.out.println("长度百分比="+randomRequestTimes);
        系统相关类.发送广播("test",1,"开始准备课件 稍等 已处理：0%");


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i<=randomRequestTimes; i+=2){
            HashMap<String, String> data = new HashMap<String, String>();
            /*
            courseOpenId: flz1asgsma1purggpgp38w
            openClassId: y5nasosztka6kzdzgnmw
            cellId: 6z11asgsyrvpyviawu0hqa
            cellLogId:
            picNum: 264
            studyNewlyTime: 12687
            studyNewlyPicNum: 264
            token: qhlyagqsurnj67oxjlpiiw
             */
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
            data.put("courseOpenId", this.courseOpenId);
            data.put("openClassId", this.openClassId);
            data.put("cellId",cellId);
            data.put("cellLogId","");
            data.put("picNum",picNum+"");
            data.put("studyNewlyTime",studyNewlyTime+"");
            data.put("studyNewlyPicNum",studyNewlyPicNum+"");
            data.put("token",token);
            zHttpRequest getunitdata=null;

            picNum += Math.round(300 / randomRequestTimes);
            studyNewlyTime += Math.round(12640 / randomRequestTimes);
            studyNewlyPicNum += Math.round(300 / randomRequestTimes);




            try {
//                Thread.sleep(100);
                getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
//                this.message =this.message + "\n"+getunitdata.getData();




//                系统相关类.发送广播("test",1,getunitdata.getData());
//                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
//                this.message =this.message + "\n提交视频进度异常 :) ...";
                系统相关类.发送广播("test",1,"提交视频进度异常 :) ...");
            }
        }

        系统相关类.发送广播("test",1,"已处理100%");



    }

    /**
     * @desc  处理视频1.0  工作中
     * @param cellId      本节课的id
     * @param token       本节课的操作token
     * @param videoTimeTotalLong    长度
     */
    public  void videodispose(String cellId,String token,String videoTimeTotalLong,int stuStudyNewlyTime){
        /**
         * 处理视频
         * courseOpenId
         * openClassId
         * cellId
         * token
         * videoTimeTotalLong
         * stuStudyNewlyTime  学生以看的进度
         */

        double[] sun={0.134831, 0.234831, 0.334831, 0.434831, 0.534831, 0.634831};
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        Random r = new Random();

        System.out.println("视频总长度："+videoTimeTotalLong);
        System.out.println("学生以看长度："+stuStudyNewlyTime);



        if(stuStudyNewlyTime>Float.parseFloat(videoTimeTotalLong)){
            stuStudyNewlyTime = (int)Float.parseFloat(videoTimeTotalLong)-20;
        }


        for (int i = stuStudyNewlyTime; i<Float.parseFloat(videoTimeTotalLong); i+=10){
            double videotime=i+sun[r.nextInt(sun.length)];
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("courseOpenId", this.courseOpenId);
            data.put("openClassId", this.openClassId);
            data.put("cellId",cellId);
            data.put("cellLogId","");
            data.put("picNum","0");
            data.put("studyNewlyTime",videotime+"");
            data.put("studyNewlyPicNum","0");
            data.put("token",token);
            zHttpRequest getunitdata=null;

            try {
                Thread.sleep(5000);
                getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
//                this.message =this.message + "\n"+getunitdata.getData();
                系统相关类.发送广播("test",1,getunitdata.getData());
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
//                this.message =this.message + "\n提交视频进度异常 :) ...";
                系统相关类.发送广播("test",1,"提交视频进度异常 :) ...");
            }


        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("cellLogId","");
        data.put("picNum","0");
        data.put("studyNewlyTime",videoTimeTotalLong);
        data.put("studyNewlyPicNum","0");
        data.put("token",token);
        zHttpRequest getunitdata=null;

        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
//            return getunitdata.getData();
//            this.message+="\n"+getunitdata.getData();
            系统相关类.发送广播("test",1,getunitdata.getData());
//            this.message+="\n视频处理完成..";
            系统相关类.发送广播("test",1,"视频处理完成..");
        } catch (Exception e) {
            e.printStackTrace();
//            this.message+="\n提交视频完结异常...";
            系统相关类.发送广播("test",1,"提交视频完结异常...");
//            return "提交视频完结异常...";
        }





    }



    public JSONObject getppt(String cellId,String moduleId){
        /**
         * 获取ppt详细信息
         *
         */
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");


        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("flag","s");
        data.put("moduleId",moduleId);

        zHttpRequest getunitdata=null;
        System.out.println(data);

        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/viewDirectory", "POST",headers,this.cookie, data);
            JSONObject retdata = new JSONObject(getunitdata.getData());
            String resUrl = retdata.getString("resUrl");
            JSONObject JSONresUrl = new JSONObject(resUrl);
            int pageCount = JSONresUrl.getJSONObject("args").getInt("page_count");
            System.out.println("获取到的ppt页数为："+pageCount);
            retdata.put("pageCount",pageCount);
            return  retdata;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    public void pptdispose(String cellId,String token,String pageCount){
        /**
         * 处理文档
         */
        HashMap<String, String> headers = new HashMap<String, String>();
        System.out.println("ppt页数："+pageCount);
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        for (int i=0;i< Float.parseFloat(pageCount);i+=10){
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("courseOpenId", this.courseOpenId);
            data.put("openClassId", this.openClassId);
            data.put("cellId",cellId);
            data.put("cellLogId","");
            data.put("picNum",i+"");
            data.put("studyNewlyTime","0");
            data.put("studyNewlyPicNum",i+"");
            data.put("token",token);

            zHttpRequest getunitdata=null;
            try {
                Thread.sleep(1000);
                getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
//                this.message =this.message + "\n"+getunitdata.getData();
                系统相关类.发送广播("test",1,getunitdata.getData());
                Thread.sleep(6000);
            } catch (Exception e) {
                e.printStackTrace();
//                this.message =this.message + "\n提交文档进度异常 :) ...";
                系统相关类.发送广播("test",1,"提交文档进度异常 :) ...");
            }
        }

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("cellLogId","");
        data.put("picNum",pageCount);
        data.put("studyNewlyTime","0");
        data.put("studyNewlyPicNum",pageCount);
        data.put("token",token);

        zHttpRequest getunitdata=null;
        try {
            Thread.sleep(6000);
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
//            return getunitdata.getData();

//            this.message+="\n"+getunitdata.getData();
            系统相关类.发送广播("test",1,getunitdata.getData());
//            this.message+="\n文档处理完成..";
            系统相关类.发送广播("test",1,"文档处理完成..");
        } catch (Exception e) {
            e.printStackTrace();
//            return "\n提交文档完结异常 :) ...";
//            this.message =this.message + "\n提交文档完结异常 :) ...";
            系统相关类.发送广播("test",1,"提交文档完结异常 :) ...");
        }
    }


    /**
     * @desc 没用
     * @param cellId
     * @param token
     */
    public void Getpptdisposeinfo(String cellId,String token){
        /**
         * 文档 - 钓出真实数据
         */
        HashMap<String, String> headers = new HashMap<String, String>();
        System.out.println("ppt欺骗000：");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("cellLogId","");
        data.put("picNum","1");
        data.put("studyNewlyTime","0");
        data.put("studyNewlyPicNum","1");
        data.put("token",token);

        zHttpRequest getunitdata=null;
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("cellLogId","");
        data.put("picNum","1");
        data.put("studyNewlyTime","0");
        data.put("studyNewlyPicNum","1");
        data.put("token",token);


        try {
            new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/stuProcessCellLog", "POST",headers,this.cookie, data);

        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    /**
     * @desc 更新职教云的cookie
     */
    public boolean getcookie(){
        String url = "http://api.qiufengvip.top/newapi/Login";

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("token", this.token);

        zHttpRequest getdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            系统相关类.发送广播("test",1,"开始更新职教云cookie");
            getdata = new zHttpRequest(url, "POST",headers,this.cookie, data);

            String yuandata = getdata.getData();


//            String as = "{\"code\":\"200\",\"msg\":\"\\u9a8c\\u8bc1\\u6210\\u529f\",\"user\":\"196307575\",\"userid\":\"vmttaw2lbpbbtccgjxhra\",\"cookie\":\"acw_tc=2f624a1d16047350678751595e313c8fd2ad964d0c4183d2d640bfb3f3a704;auth=0102D94E84F4F082D808FED95E30C64483D808011576006D00740074006100770032006C00620070006200620074006300630067006A00780068007200610000012F00FFC443CD8E447C9E6699D4793DF75BD0E60CF167B9\",\"newtoken\":\"@b1ac2e8193a04a1caee1d320a6a85e1a\"}\n";

            String code =getSubString(yuandata,"\"code\":\"","\"");
            String cookie = getSubString(yuandata,"\"cookie\":\"","\"");
            String newToken = getSubString(yuandata,"\"newtoken\":\"","\"");
            String userId = getSubString(yuandata,"\"userid\":\"","\"");



            if (code.equals("200")){
                this.cookie = cookie;
                this.newToken = newToken;
                this.userId =userId;
                系统相关类.发送广播("test",1,"更新职教云cookie成功");
                return true;
            }else {
                系统相关类.发送广播("test",1,"职教管家账号异地登录，长时间不更新cookie会导致刷课程序退出");
                return false;
            }


        } catch (Exception e) {
            e.printStackTrace();
            系统相关类.发送广播("test",1,"更新cookie未能成功");
            return false;
        }
    }


    /**
     * 取两个文本之间的文本值
     * @param text 源文本 比如：欲取全文本为 12345
     * @param left 文本前面
     * @param right 后面文本
     * @return 返回 String
     */
    public static String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }








    public String statement(String moduleId,String cellId,String cellName){
        /**
         * 声明操作域
         */
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("moduleId",moduleId);
        data.put("cellId",cellId);
        data.put("cellName",cellName);

        zHttpRequest getunitdata=null;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        try {
            getunitdata = new zHttpRequest("https://zjy2.icve.com.cn/api/common/Directory/changeStuStudyProcessCellData", "POST",headers,this.cookie, data);
            return getunitdata.getData();
        } catch (Exception e) {
            e.printStackTrace();
            系统相关类.发送广播("test",1,"声明操作域异常");
            return "声明操作域异常";


        }

    }

    /**
     * @desc 判定本用户有没有[评价]过该课件
     * @param cellId
     * @param Type     类型 1= 评价  2= 问答  3 = 笔记 4 = 纠错
     * @return 布尔值
     */
    private boolean Didcomment(String cellId,int Type){
        HashMap<String, String> data = new HashMap<String, String>();
        data.put("courseOpenId", this.courseOpenId);
        data.put("openClassId", this.openClassId);
        data.put("cellId",cellId);
        data.put("type","1");


        data.put("pageSize","5000");
        zHttpRequest getcommentdata=null;

        String Url = "";
        switch (Type){
            case 1:  //评价

                Url = "https://zjy2.icve.com.cn/api/common/Directory/getCellCommentData";
                data.put("type","1");
                break;
            case 2:  //问答
                Url = "https://zjy2.icve.com.cn/api/common/Directory/getCellCommentData";
                data.put("type","3");
                break;
            case 3:  //笔记
                Url = "https://zjy2.icve.com.cn/api/common/Directory/getCellCommentData";
                data.put("type","2");
                break;
            case 4:  //纠错
                Url = "https://zjy2.icve.com.cn/api/common/Directory/getCellCommentData";
                data.put("type","4");
                break;
            default:
                return true;
        }



        HashMap<String, String> headers = new HashMap<String, String>();

        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");

        try {
            System.out.println(Url);
            getcommentdata = new zHttpRequest(Url, "POST", headers, this.cookie, data);

            JSONObject  JsonGetComment = new JSONObject(getcommentdata.getData());
            JSONArray List =JsonGetComment.getJSONArray("list");
            Set<String>  StudentList = new HashSet<String>();
            for (int i=0;i<List.length(); i++) {

                JSONObject JSONList = List.getJSONObject(i);
//                System.out.println(JSONList);
                String UsetId = JSONList.getString("userId");
                StudentList.add(UsetId);
            }
            return StudentList.add(this.userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("请求异常");
        }

        return true;
    }




    /**
     * private boolean iscomment;  //开启评论
     * private boolean iserror;  //开启纠错
     * private boolean isissue;  //问答
     */
    public void comment(String cellId)  {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.87 Safari/537.36");
        if (this.iscomment){
            //开启评论

            if (!this.Didcomment(cellId,1)){//判定 是否有本操作信息
                系统相关类.发送广播("test",1,"本课件已经评论过了，自动退出评论");
            }else {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Random ra = new Random();
                String url = "https://zjyapp.icve.com.cn/newmobileapi/bbs/addCellComment";
                System.out.println("开始添加评论...");

                String[] commentinfo = {
                        "此课程讲的非常好。",
                        "此课程采用多媒体教学设施使讲课效果良好。",
                        "老师讲的很好",
                        "课件不错，点赞",
                        "讲的不错，都明白了，点赞！"
                };

                String comment = commentinfo[ra.nextInt(5)];
                String data_info = "{\"OpenClassId\":\"" + this.openClassId + "\",\"CourseOpenId\":\"" + this.courseOpenId + "\",\"CellId\":\"" + cellId + "\",\"UserId\":\"" + userId + "\",\"Content\":\"" + comment + "\",\"SourceType\":2,\"Star\":5.0}";
                系统相关类.发送广播("test", 1, "开始添加评论(内容为：" + comment + ")...");
                HashMap<String, String> data = new HashMap<String, String>();
                data.put("data", data_info);
                data.put("newToken", this.newToken);
                zHttpRequest getunitdata = null;
                try {
                    getunitdata = new zHttpRequest(url, "POST", headers, this.cookie, data);
                    System.out.println(getunitdata.getData());
                    系统相关类.发送广播("test", 1, getunitdata.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("评论异常...");
                    系统相关类.发送广播("test", 1, "评论异常...");
                }
            }
        }
        if (this.iserror){
            //开启纠错
            if (!this.Didcomment(cellId,4)){//判定 是否有本操作信息
                系统相关类.发送广播("test",1,"本课件已经有过纠错了，自动退出");
            }else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String url = "https://zjyapp.icve.com.cn/newmobileapi/bbs/addCellError";
                System.out.println("开始添加纠错...");
                系统相关类.发送广播("test", 1, "开始添加纠错(内容为：无)...");
                String data_info = "{\"OpenClassId\":\"" + this.openClassId + "\",\"CourseOpenId\":\"" + this.courseOpenId + "\",\"CellId\":\"" + cellId + "\",\"UserId\":\"" + userId + "\",\"Content\":\"无\",\"SourceType\":2}";

                HashMap<String, String> data = new HashMap<String, String>();
                data.put("data", data_info);
                data.put("newToken", this.newToken);
                zHttpRequest getunitdata = null;
                try {
                    getunitdata = new zHttpRequest(url, "POST", headers, this.cookie, data);
                    System.out.println(getunitdata.getData());
                    系统相关类.发送广播("test", 1, getunitdata.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("添加纠错异常...");
                    系统相关类.发送广播("test", 1, "添加纠错异常...");
                }
            }
        }
        if (this.isissue){
            //开启问答
            if (!this.Didcomment(cellId,2)){//判定 是否有本操作信息
                系统相关类.发送广播("test",1,"本课件已经有过问答了，自动退出");
            }else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String url = "https://zjyapp.icve.com.cn/newmobileapi/bbs/addCellAskAnswer";
                System.out.println("开始添加问答...");
                系统相关类.发送广播("test", 1, "开始添加问答(内容为：无)...");
                String data_info = "{\"OpenClassId\":\"" + this.openClassId + "\",\"CourseOpenId\":\"" + this.courseOpenId + "\",\"CellId\":\"" + cellId + "\",\"UserId\":\"" + userId + "\",\"Content\":\"无\",\"SourceType\":2}";

                HashMap<String, String> data = new HashMap<String, String>();
                data.put("data", data_info);
                data.put("newToken", this.newToken);
                zHttpRequest getunitdata = null;
                try {
                    getunitdata = new zHttpRequest(url, "POST", headers, this.cookie, data);
                    System.out.println(getunitdata.getData());
                    系统相关类.发送广播("test", 1, getunitdata.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("添加问答异常...");
                    系统相关类.发送广播("test", 1, "添加问答异常...");
                }
            }
        }


        /**
         * 笔记：内容好
         */
        if (this.isCellNote) {
            //开启笔记
            if (!this.Didcomment(cellId, 3)) {//判定 是否有本操作信息
                系统相关类.发送广播("test", 1, "本课件已经有过笔记了，自动退出");
                return;
            } else {
                String url = "https://zjyapp.icve.com.cn/newmobileapi/bbs/addCellNote";
                System.out.println("开始添加笔记...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                系统相关类.发送广播("test", 1, "开始添加笔记(内容为：好)...");
                String data_info = "{\"OpenClassId\":\"" + this.openClassId + "\",\"CourseOpenId\":\"" + this.courseOpenId + "\",\"CellId\":\"" + cellId + "\",\"UserId\":\"" + userId + "\",\"Content\":\"好\",\"SourceType\":2}";

                HashMap<String, String> data = new HashMap<String, String>();
                data.put("data", data_info);
                data.put("newToken", this.newToken);
                zHttpRequest getunitdata = null;
                try {
                    getunitdata = new zHttpRequest(url, "POST", headers, this.cookie, data);
                    System.out.println(getunitdata.getData());
                    系统相关类.发送广播("test", 1, getunitdata.getData());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("添加笔记异常...");
                    系统相关类.发送广播("test", 1, "添加笔记异常...");
                }
            }
        }
    }
}









class 系统相关类 {
    public static void 发送广播(String name, int aaaa, String info){
        System.out.println(info);

    }
}








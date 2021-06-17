package test;

import com.qiufeng.ZjyCourseware;

public class TestCourses {
    public static void main(String[] args) {
        System.out.println("开始");
        ZjyCourseware zjyCourseware= null;
        try {
            zjyCourseware = new ZjyCourseware();
        } catch (Exception e) {
            e.printStackTrace();
        }
        zjyCourseware.初始化("35263cc8e675d7051d54a532b65b2fcb","ejhaacgeuxa","a8sgaoxfmya",false,false,false,false,true);
        Thread zjyThread = new Thread(zjyCourseware);
        zjyThread.start();
    }
}


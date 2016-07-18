package org.pqh.test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.runner.RunWith;
import org.pqh.dao.BiliDao;
import org.pqh.dao.VstorageDao;
import org.pqh.entity.Data;
import org.pqh.entity.Vstorage;
import org.pqh.service.AvCountService;
import org.pqh.service.InsertService;
import org.pqh.service.InsertServiceImpl;
import org.pqh.util.BiliUtil;
import org.pqh.util.Constant;
import org.pqh.util.FindResourcesUtil;
import org.pqh.util.TestSlf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Reborn on 2016/2/5.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class Test {
    private static Logger log= TestSlf4j.getLogger(Test.class);
    @Resource
    ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    BiliDao biliDao;
    @Resource
    VstorageDao vstorageDao;
    @Resource
    InsertService insertService;
    @Resource
    AvCountService avCountService;

    public static void main(String[] args) throws Exception {
        Test test=new Test();
        //getUrl("669933500564212");
//        test.saveDataBase();
//        String filename=FindResourcesUtil.switchFileName(btAcg.getResourceName());
//        FindResourcesUtil.downLoadTorrent("http://www.kuaipic.com/uploads/userup/231761/ef13541d77e923aeb125.jpg", FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath());
//        FindResourcesUtil.downLoad("http://comment.bilibili.com/5275150.xml","abc/test.xml");

    }

    /**
     * 测试各种方法
     */
    @org.junit.Test
    public void testMethod() {
//        Map<String,List<BtAcg>> map=FindResourcesUtil.findBy_Btacg(threadPoolTaskExecutor,"银魂");
//        Map<String,String> hrefMap=FindResourcesUtil.screenUrl(map,new BtAcg("BDRIP",null,null));
//            BiliUtil.createConfig(biliDao,new File("src/config.properties"));
//        downLoadDanMu("%权力的游戏%",biliDao);
        Map<String,String> map=new HashMap<String, String>();
        map.put("title","权力的游戏");
        List<Data> list=vstorageDao.selectData(map);
    }

    public void checkId(Map<String,String> map){
        int count=0;
        for(String key:map.keySet()) {
            String error=map.get(key)+"：不合法ID参数,ID参数正确格式应该是纯数字，如果是多个ID则数字之间要用逗号隔开";
            if(map.get(key).indexOf(",")==-1&&map.get(key).replaceAll("\\D","").length()==0){
                throw new RuntimeException(error);
            }
            for (String s : map.get(key).split(",")) {
                if (key.contains("id")&&s.replaceAll("\\D", "").length() == 0) {
                    throw new RuntimeException(error);
                }else{
                    count++;
                }
            }
        }
        log.info("共检测出"+count+"个ID准备拼接到sql语句里面进行查询");
    }
    /**
     * 查询条件
     * @param map
     * @param biliDao
     */
    public void downLoadDanMu(Map<String,String> map,BiliDao biliDao){
        long a=System.currentTimeMillis();
        this.checkId(map);
        List<Data> dataList=vstorageDao.selectData(map);
        long b=System.currentTimeMillis();
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(b-a);
        log.info("查询耗费时间"+calendar.get(Calendar.MINUTE)+"m\t"+calendar.get(Calendar.SECOND)+"s");
        Map<String,List<Data>> listMap=new HashMap<String, List<Data>>();
        for(Data data:dataList){
            String dirname= FindResourcesUtil.switchFileName(data.getTitle());
            if(listMap.get(dirname)==null){
                listMap.put(dirname,new ArrayList<Data>());
            }
            listMap.get(dirname).add(data);
        }
        for(String dirName:listMap.keySet()){
            dataList=listMap.get(dirName);
            for(Data data:dataList){
                String path=dirName;
                if(dataList.size()>1){
                    if(data.getSubtitle()!=null){
                        path+="/"+FindResourcesUtil.switchFileName(data.getSubtitle());
                    }else{
                        path+="/"+data.getCid()+"";
                    }
                }
                FindResourcesUtil.downLoad("http://comment.bilibili.com/"+data.getCid()+".xml","弹幕/"+path+".xml");
            }
            File file=new File(dirName);
            if(file.isDirectory()) {
                int fileCount = file.listFiles().length;
                if (fileCount == 0) {
                    try {
                        FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        TestSlf4j.outputLog(e,log);
                    }
                }
            }
        }
    }
    /**
     * 获取动画开播日期
     * @param document
     * @return
     */
    public static String getInfo(Document document){
        if(document.select("ul.polysemantList-wrapper .selected").text().contains("动画")){
            Elements elements=document.select("div.basic-info>dl>dt");
            for(Element element:elements){
                if(element.text().equals("播放期间")){
                    int index=elements.indexOf(element);
                    element=document.select("div.basic-info>dl>dd").get(index);
                    return element.text();
                }
            }
        }
        Elements elements=document.select("ul.polysemantList-wrapper>.item>a");
        for(Element element:elements){
            if(element.attr("title").length()!=0&&element.attr("title").contains("动画")){
                log.info(element.attr("title")+"跳转到动画条目"+Constant.BAIKEINDEX+element.attr("href"));
                return getInfo(BiliUtil.jsoupGet(Constant.BAIKEINDEX+element.attr("href"), Document.class, Constant.GET));
            }
        }
        return "";
    }

    /**
     * 压缩备份文件
     * @param _7zFile 压缩包文件
     * @param sqlFile 数据库文件
     */
    public static void compress(File _7zFile,File sqlFile){
        List<String> list=new ArrayList<String>();
        list.add("7z a -t7z "+_7zFile.getAbsolutePath()+" "+sqlFile.getAbsolutePath()+" -mx=9 -m0=LZMA2:a=2:d=26 -ms=4096m -mmt -pA班姬路");
        File file=new File(sqlFile.getParent()+"\\Test.bat");
        try {
            FileUtils.writeLines(file,"GBK",list);
        } catch (IOException e) {
            TestSlf4j.outputLog(e,log);
        }
        runCommand(file.getAbsolutePath());
        file.delete();
    }

    /**
     * 删除旧的备份文件
     * @param date 比较的时间
     * @param dir 备份文件目录
     */
    public  static void delOldFile(Date date,String dir){
        Collection<File> fileList=FileUtils.listFiles(new File(dir),new String[]{"sql"},true);
        for(File file:fileList){
            if(FileUtils.isFileOlder(file,date)){
                log.info("删除旧备份文件"+file.getAbsoluteFile());
                file.delete();
            }
        }
    }

    /**
     * 日期格式化
     * @param date 日期
     * @param format 格式
     * @return 返回格式化日期
     */
    public static String format(Date date,String format){
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 删除junit产生的临时文件
     */
    public void deleteTestTemp(){
        //临时文件，选中扩展名为out格式的文件
        Collection<File> fileList=FileUtils.listFiles(FileUtils.getTempDirectory(),new String[]{"out"},true);
        for(File file:fileList){
            //确认是idea产生的临时文件则删除
            if(file.getName().contains("idea")){
                file.delete();
            }
        }
    }

    /**
     * 备份数据库
     */
    public  void saveDataBase(){
        BiliUtil.openImage(new File("WebContent/dbbackup.jpg"));
        List<String> list=new ArrayList<String>();
        Date date=new Date();
        String date_1=format(date,"HH_mm_ss");
        String date_2=format(date,"yyyy_MM_dd");
        Calendar c=Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE,-1);
        Date date1=c.getTime();
        String date_3=format(date1,"yyyy_MM_dd");
        //当前日期年月日作为备份数据库的目录
        String todayDir=BiliUtil.getPropertie("localPath")+date_2+"\\";
        String yesterday=BiliUtil.getPropertie("localPath")+date_3+"\\";
        //当前日期时分秒作为备份数据库文件的文件名
        File sqlFile=new File(todayDir+date_1+".sql");
        File oldDir=new File(yesterday);
        //调用mysqldump备份命令备份数据库
        list.add("\"K:\\MySQL\\MySQL Server 5.7\\bin\\mysqldump\" --opt -uroot -p123456 bilibili "+BiliUtil.getPropertie("backuptables")+"> "+sqlFile.getAbsolutePath());
        File batFile=new File(todayDir+date_1+".bat");
        try {
            FileUtils.writeLines(batFile,"GBK",list);
        } catch (IOException e) {
            TestSlf4j.outputLog(e,log);
        }
        //命令写进文件进行备份
        Test.runCommand(batFile.getAbsolutePath());
        //命令结束删除
        batFile.delete();
        delOldFile(date,todayDir);
        //每天凌晨三点打包一次数据库放到服务器
        File _7zFile=new File(BiliUtil.getPropertie("serverPath")+date_2+"\\"+date_1+".7z");
        compress(_7zFile,sqlFile);
        File old7zFile=new File(BiliUtil.getPropertie("serverPath")+date_3+"\\");
        FileUtils.deleteQuietly(old7zFile);
        FileUtils.deleteQuietly(oldDir);

    }

    /**
     * 调用命令行运行命令
     * @param command 运行命令
     */
    public static void runCommand(String command){
        Process ps = null;
        try {
            long a=System.currentTimeMillis();
            ps=Runtime.getRuntime().exec(command);
            InputStreamReader i = new InputStreamReader(ps.getInputStream(),"GBK");
            String line;
            BufferedReader ir = new BufferedReader(i);
            while ((line = ir.readLine()) != null) {
                if(line.length()>0) {
                    log.info(line);
                }
            }
            long b=System.currentTimeMillis();
            log.info("运行命令花费时间"+(b-a)+"ms");
        } catch (IOException e) {
            TestSlf4j.outputLog(e,log);
        }
    }
    /**
     * 为对象的指定属性赋值
     * @param object
     * @param key
     * @param value
     * @return
     */
    public Object setObject(Object object,String key,String value) {
        Field field=null;
        try {
            field = object.getClass().getDeclaredField(key);
        }catch (NoSuchFieldException e) {
            try {
                field = object.getClass().getSuperclass().getDeclaredField(key);
            } catch (NoSuchFieldException e1) {
                TestSlf4j.outputLog(e1,log);
            }
        }
        field.setAccessible(true);
        String type = field.getType().getName();
        try {
            if (type.equals("java.lang.Integer")) {
                field.set(object, Integer.parseInt(value));
            } else if (type.equals("java.lang.Long")) {
                field.set(object, Long.parseLong(value));
            } else if (type.equals("java.lang.Float")) {
                field.set(object, Float.parseFloat(value));
            } else if (type.equals("java.lang.Boolean")) {
                field.set(object, Boolean.parseBoolean(value));
            } else if (type.equals("java.util.Date")) {
                if (value.contains(":")) {
                    field.set(object, new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(value));
                } else {
                    field.set(object, new SimpleDateFormat("yyyy-MM-dd").parse(value));
                }
            } else {
                field.set(object, value);
            }
        }
        catch (NumberFormatException e){
            if(e.getMessage().equals("For input string: \"\"")){
                try {
                    field.set(object,null);
                } catch (IllegalAccessException e1) {
                    TestSlf4j.outputLog(e,log);
                }
            }
        }
        catch (ParseException e) {
            TestSlf4j.outputLog(e,log);
        } catch (IllegalAccessException e) {
            TestSlf4j.outputLog(e,log);
        }
        return object;
    }

    /**
     * 获取子节点
     * @param classname
     * @param Field
     * @return
     */
    public String getChildNode(String classname,String Field){
        Field [] fields=null;
        try {
            fields=Class.forName(classname).getDeclaredFields();
            for(Field field:fields){
                if(field.getName().equals(Field)){
                    if(field.getType().getName().equals("java.util.List")){
                        String genericType = field.getGenericType().toString().replaceAll("java.util.List<|>","");
                        return genericType;
                    }else {
                        return field.getType().getName();
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            TestSlf4j.outputLog(e,log);
        }
        return null;
    }

    /**
     * 获取父节点
     * @return
     */
    public String getParentsNode(String classname){
        Class c= null;
        try {
            c = Class.forName(classname);
            return c.getMethod("getParents").invoke(c.newInstance()).toString();
        } catch (ClassNotFoundException e) {
            TestSlf4j.outputLog(e,log);
        } catch (NoSuchMethodException e) {
            TestSlf4j.outputLog(e,log);
        } catch (IllegalAccessException e) {
            TestSlf4j.outputLog(e,log);
        } catch (InstantiationException e) {
            TestSlf4j.outputLog(e,log);
        } catch (InvocationTargetException e) {
            TestSlf4j.outputLog(e,log);
        }
        return null;
    }

    /**
     * 根据类名获取对象
     * @param classname
     * @return
     */
    public Object getClass(String classname){
        try {
            return Class.forName(classname).newInstance();
        } catch (InstantiationException e) {
            TestSlf4j.outputLog(e,log);
        } catch (IllegalAccessException e) {
            TestSlf4j.outputLog(e,log);
        } catch (ClassNotFoundException e) {
            TestSlf4j.outputLog(e,log);
        }
        return null;
    }

    /**
     * 把json对象存进map里面
     * @param jsonObject json对象
     * @param map json对象转换的实体类字典
     * @param classname 根节点名称
     * @param flag
     * @param index
     * @param cid
     * @return
     */
    public Map getMap(JSONObject jsonObject,Map map,String classname,boolean flag,int index,int cid){
        for(Object key:jsonObject.keySet()) {
            if (JSONUtils.isArray(jsonObject.get(key))) {
                JSONArray jsonArray = jsonObject.getJSONArray(key.toString());
                boolean flag_1=getChildNode(classname,key.toString()).equals(String.class.getName());
                classname = flag_1?classname:getChildNode(classname,key.toString());
                if(flag_1){
                    String value=parseByJackson(Constant.VSTORAGEAPI+cid,key.toString());
                    map.put(classname, setObject(map.get(Data.class.getName()), key.toString(),value));
                    continue;
                }
                for (int i=0;i<jsonArray.size();i++) {
                    index = i;
                    ((List)map.get(classname)).add(getClass(classname));
                    getMap(JSONObject.fromObject(jsonArray.get(i)), map, classname,true,index,cid);
                    setObject(((List)map.get(classname)).get(index),"cid",String.valueOf(cid));
                    setObject(((List)map.get(classname)).get(index),"id",String.valueOf(i+1));
                }
                classname=getParentsNode(classname);
            } else if (JSONUtils.isObject(jsonObject.get(key))&&!jsonObject.get(key).equals(null)) {
                JSONObject object = jsonObject.getJSONObject(key.toString());
                JSONObject jsonObject1 = jsonObject;
                String name = classname;
                classname = getChildNode(classname,key.toString());
                getMap(object, map, classname,false,index,cid);
                setObject(map.get(classname),"cid",String.valueOf(cid));
                jsonObject = jsonObject1;
                classname = getParentsNode(classname);
            } else {
                String value = jsonObject.get(key).toString();
                if(flag){
                    Object o=((List)map.get(classname)).get(index);
                    setObject(o, key.toString(), value);
                }else {
                    map.put(classname, setObject(map.get(classname), key.toString(), value));
                }
            }
        }
        if(classname.equals(Vstorage.class.getName())) {
            map.put(classname, setObject(map.get(classname), "id", String.valueOf(cid)));
        }
        return map;
    }

    public void setData(VstorageDao vstorageDao,Map<String,Object> map){
        Class c=vstorageDao.getClass();
        String name=null;
        String classnames[] = BiliUtil.getPropertie("exclude").split(",");
        for(String classname:classnames){
            map.remove(classname);
        }

        for(String key:map.keySet()) {
            Method insertMethod = null;
            Method updateMethod = null;
            try {
                name = key.substring(key.lastIndexOf(".") + 1);
                insertMethod = c.getDeclaredMethod("insert" + name, Class.forName(key));
                updateMethod = c.getDeclaredMethod("update" + name, Class.forName(key));
            } catch (NoSuchMethodException e) {
                TestSlf4j.outputLog(e,log);
            } catch (ClassNotFoundException e) {
                TestSlf4j.outputLog(e,log);
            }
            if (map.get(key).getClass().getName().contains("List")) {
                List list = (List) map.get(key);
                if (list.size() == 0) {
                    continue;
                }
                for (Object object : (List) map.get(key)) {
                    if (checkfieldsNaN(object)) {
                        continue;
                    }
                    try {
                        insertMethod.invoke(vstorageDao, object);
                    } catch (InvocationTargetException e) {
                        Field field=null;
                        String detailMessage=null;
                        try {
                            field=Throwable.class.getDeclaredField("detailMessage");
                            field.setAccessible(true);
                            detailMessage=field.get(e.getTargetException().getCause()).toString();
                            detailMessage=BiliUtil.matchStr(detailMessage,"\\d+\\-\\d+",String.class);
                            if(detailMessage.length()!=0){
                                log.info("更新"+name+"复合主键："+detailMessage+"信息");
                                updateMethod.invoke(vstorageDao, object);
                            }
                        } catch (NoSuchFieldException e1) {
                            log.error(object+"无法获取详细报错信息！！！");
                        } catch (IllegalAccessException e1) {
                            TestSlf4j.outputLog(e1,log);
                        } catch (InvocationTargetException e1) {
                            TestSlf4j.outputLog(e1,log);
                        }
                    } catch (IllegalAccessException e) {
                        TestSlf4j.outputLog(e,log);
                    }
                }

            } else {
                if (checkfieldsNaN(map.get(key))) {
                    continue;
                }
                try {
                    insertMethod.invoke(vstorageDao, map.get(key));
                } catch (InvocationTargetException e) {
                    if(e.getTargetException().getClass().equals(DuplicateKeyException.class)){
                        log.info("更新"+name+"主键："+key+"信息");
                    }else{
                        TestSlf4j.outputLog(e,log);
                    }
                } catch (IllegalAccessException e) {
                    TestSlf4j.outputLog(e,log);
                }
            }
        }
    }

    public boolean checkfieldsNaN(Object object){
        Class _class= null;
        Field fields[]=null;
        try {
            _class = object.getClass();
            fields=_class.getDeclaredFields();
            for(Field field:fields){
                field.setAccessible(true);
                if(field.get(object)!=null){
                    return false;
                }
            }
            return true;
        } catch (IllegalAccessException e) {
            TestSlf4j.outputLog(e,log);
        }
        return true;
    }

    public static String parseByJackson(String url,String key) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map data = objectMapper.readValue(new URL(url), Map.class);
            Map map1 = null;
            if (data.get("data") != null) {
                map1 = ((Map) data.get("data"));
            }
            if (map1 != null) {
                return map1.get(key) == null ? "" : map1.get(key).toString();
            }

        } catch (JsonParseException e) {
            TestSlf4j.outputLog(e,log);
        } catch (JsonMappingException e) {
            TestSlf4j.outputLog(e,log);
        } catch (MalformedURLException e) {
            TestSlf4j.outputLog(e,log);
        } catch (IOException e) {
            TestSlf4j.outputLog(e,log);
        }
        return  null;
    }

    public int restoreCid(int id){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            TestSlf4j.outputLog(e,log);
        }
        threadPoolTaskExecutor.getThreadPoolExecutor().getQueue().clear();
        InsertServiceImpl.count=0;
        return biliDao.getAid(id);
    }

    @org.junit.Test
    public void testVstorage() {
        int speed=500;
        for (int cid = biliDao.getAid(3); ;cid++) {
            if(InsertServiceImpl.count>=10){
                cid=restoreCid(3);
                speed=500;
            }else{
                speed=100;
            }
            TaskVstorage taskVstorage=new TaskVstorage(cid,insertService);
            excute(threadPoolTaskExecutor,taskVstorage,speed);
        }
    }

    @org.junit.Test
    public void testThread(){
        int speed=500;
        for(int cid=biliDao.getAid(2);;cid++){
            if(InsertServiceImpl.count>=10){
                cid=restoreCid(2);
                speed=500;
            }else{
                speed=100;
            }
            TaskCid taskCid=new TaskCid(cid,insertService);
            excute(threadPoolTaskExecutor,taskCid,speed);
        }

    }

    public void excute(ThreadPoolTaskExecutor threadPoolTaskExecutor,Runnable runnable,int speed){
        try {
            threadPoolTaskExecutor.execute(runnable);
            Thread.sleep(speed);
        }
        catch (TaskRejectedException e2){
            log.error("队列任务已满，线程休息1小时");
            try {
                Thread.sleep(3600000);
            } catch (InterruptedException e) {
                TestSlf4j.outputLog(e,log);
            }
        }
        catch (InterruptedException e) {
            TestSlf4j.outputLog(e,log);
        }
    }
    @org.junit.Test
    public void testView(){
        insertService.insertBili(biliDao.getAid(1),1);
    }

}

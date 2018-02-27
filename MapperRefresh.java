package com.maintainsys.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.apache.log4j.Logger;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import com.google.common.collect.Sets;

/**
 * 刷新MyBatis Mapper XML 线程
 * @author 原作者：ThinkGem
 * 尹晓晨修改  用来和重写的SqlSessionFactoryBean类配合，以解决mybtis xml文件无法热部署的问题
 * @version 2016-5-29
 */
public class MapperRefresh implements Runnable {

    /*初始化配置*/
    private static boolean enabled=true;         // 是否启用Mapper刷新线程功能
    private static int delaySeconds=1;        // 延迟刷新秒数
    private static int sleepSeconds=1;        // 休眠时间
    private static String mappingPath="dao";      // xml文件所在的文件夹名，需要根据需要修改


    /*==========================================================================*/
    private static Properties prop;
    private static boolean refresh;         // 刷新启用后，是否启动了刷新线程
    private Set<String> location;         // Mapper实际资源路径
    private Resource[] mapperLocations;     // Mapper资源路径
    private Configuration configuration;        // MyBatis配置对象
    private Long beforeTime = 0L;           // 上一次刷新时间

    public static Logger log = Logger.getLogger(MapperRefresh.class);
    static {
        prop = new Properties();
        System.out.println("是否开启刷新线程：" + enabled);
        System.out.println("延迟刷新秒数：" + delaySeconds + "秒");
        System.out.println("休眠时间：" + sleepSeconds + "秒");
        System.out.println("mapper文件路径：" + mappingPath);
    }

    public static boolean isRefresh() {
        return refresh;
    }

    public MapperRefresh(Resource[] mapperLocations, Configuration configuration) {
        this.mapperLocations = mapperLocations;
        this.configuration = configuration;
    }

    @Override
    public void run() {

        beforeTime = System.currentTimeMillis();

        log.debug("[location] " + location);
        log.debug("[configuration] " + configuration);

        if (enabled) {
            // 启动刷新线程
            final MapperRefresh runnable = this;
            new Thread(new Runnable() {
                @Override
                public void run() {

                    if (location == null){
                        location = Sets.newHashSet();
                        log.debug("MapperLocation's length:" + mapperLocations.length);
                        for (Resource mapperLocation : mapperLocations) {
                            String s = mapperLocation.toString().replaceAll("\\\\", "/");
                            s = s.substring("file [".length(), s.lastIndexOf(mappingPath) + mappingPath.length());
                            if (!location.contains(s)) {
                                location.add(s);
                                log.debug("Location:" + s);
                            }
                        }
                        log.debug("Locarion's size:" + location.size());
                    }

                    try {
                        Thread.sleep(delaySeconds * 1000);
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                    refresh = true;

                    System.out.println("=========  mybatis自动刷新mapper文件功能已启用 =========");

                    while (true) {
                        try {
                            for (String s : location) {
                                runnable.refresh(s, beforeTime);
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        try {
                            Thread.sleep(sleepSeconds * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }, "MyBatis-Mapper-Refresh").start();
        }
    }

    /**
     * 执行刷新
     * @param filePath 刷新目录
     * @param beforeTime 上次刷新时间
     * @throws NestedIOException 解析异常
     * @throws FileNotFoundException 文件未找到
     * @author ThinkGem
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void refresh(String filePath, Long beforeTime) throws Exception {

        // 本次刷新时间
        Long refrehTime = System.currentTimeMillis();

        // 获取需要刷新的Mapper文件列表
        List<File> fileList = this.getRefreshFile(new File(filePath), beforeTime);
        if (fileList.size() > 0) {
            log.debug("需要被刷新的Mapper文件个数: " + fileList.size()+"个");
        }
        boolean isEXistExcep;
        for (File aFileList : fileList) {
            isEXistExcep=false;//是否有异常的标记
            InputStream inputStream = new FileInputStream(aFileList);
            String resource = aFileList.getAbsolutePath();
            try {

                // 清理原有资源，更新为自己的StrictMap方便，增量重新加载
                String[] mapFieldNames = new String[]{
                        "mappedStatements", "caches",
                        "resultMaps", "parameterMaps",
                        "keyGenerators", "sqlFragments"
                };
                for (String fieldName : mapFieldNames) {
                    Field field = configuration.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Map map = ((Map) field.get(configuration));
                    if (!(map instanceof StrictMap)) {
                        Map newMap = new StrictMap(StringUtils.capitalize(fieldName) + "collection");
                        for (Object key : map.keySet()) {
                            try {
                                newMap.put(key, map.get(key));
                            } catch (IllegalArgumentException ex) {
                                newMap.put(key, ex.getMessage());
                            }
                        }
                        field.set(configuration, newMap);
                    }
                }

                // 清理已加载的资源标识，方便让它重新加载。
                Field loadedResourcesField = configuration.getClass().getDeclaredField("loadedResources");
                loadedResourcesField.setAccessible(true);
                Set loadedResourcesSet = ((Set) loadedResourcesField.get(configuration));
                loadedResourcesSet.remove(resource);

                //重新编译加载资源文件。
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(inputStream, configuration,
                        resource, configuration.getSqlFragments());
                xmlMapperBuilder.parse();
            } catch (Exception e) {
                isEXistExcep=true;
                throw new NestedIOException("解析mapper.xml资源失败: '" + resource + "'", e);
            } finally {
                ErrorContext.instance().reset();
                if (isEXistExcep){
                    System.out.println("====================文件"+aFileList.getName()+"格式存在错误，刷新失败，请检查！======================");
                    break;
                }
            }
            System.out.println("Refresh file: " + mappingPath + StringUtils.substringAfterLast(aFileList.getAbsolutePath(), mappingPath));
            if (log.isDebugEnabled()) {
                log.debug("成功刷新Mapper文件: " + aFileList.getAbsolutePath());
                log.debug("被刷新的文件名: " + aFileList.getName());
                System.out.println("===========================================================================================================");
            }
        }
        // 如果刷新了文件，则修改刷新时间，否则不修改
        if (fileList.size() > 0) {
            this.beforeTime = refrehTime;
        }
    }

    /**
     * 获取需要刷新的文件列表
     * @param dir 目录
     * @param beforeTime 上次刷新时间
     * @return 刷新文件列表
     */
    private List<File> getRefreshFile(File dir, Long beforeTime) {
        List<File> fileList = new ArrayList<File>();

        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    fileList.addAll(this.getRefreshFile(file, beforeTime));
                } else if (file.isFile()) {
                    if (this.checkFile(file, beforeTime)) {
                        fileList.add(file);
                    }
                } else {
                    System.out.println("Error file." + file.getName());
                }
            }
        }
        return fileList;
    }

    /**
     * 判断文件是否需要刷新
     * @param file 文件
     * @param beforeTime 上次刷新时间
     * @return 需要刷新返回true，否则返回false
     */
    private boolean checkFile(File file, Long beforeTime) {
        if (file.lastModified() > beforeTime&&file.getName().contains(".xml")) {
            return true;
        }
        return false;
    }

    /**
     * 获取整数属性
     * @param key
     * @return
     */
    private static int getPropInt(String key) {
        int i = 0;
        try {
            i = Integer.parseInt(getPropString(key));
        } catch (Exception e) {
        }
        return i;
    }

    /**
     * 获取字符串属性
     * @param key
     * @return
     */
    private static String getPropString(String key) {
        return prop == null ? null : prop.getProperty(key);
    }

    /**
     * 重写 org.apache.ibatis.session.Configuration.StrictMap 类
     * 来自 MyBatis3.4.0版本，修改 put 方法，允许反复 put更新。
     */
    public static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -4950446264854982944L;
        private String name;

        public StrictMap(String name, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
            this.name = name;
        }

        public StrictMap(String name, int initialCapacity) {
            super(initialCapacity);
            this.name = name;
        }

        public StrictMap(String name) {
            super();
            this.name = name;
        }

        public StrictMap(String name, Map<String, ? extends V> m) {
            super(m);
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V put(String key, V value) {
            // ThinkGem 如果现在状态为刷新，则刷新(先删除后添加)
            if (MapperRefresh.isRefresh()) {
                remove(key);
                MapperRefresh.log.debug("refresh key:" + key.substring(key.lastIndexOf(".") + 1));
            }
            // ThinkGem end
            if (containsKey(key)) {
                throw new IllegalArgumentException(name + " already contains value for " + key);
            }
            if (key.contains(".")) {
                final String shortKey = getShortName(key);
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    super.put(shortKey, (V) new Ambiguity(shortKey));
                }
            }
            return super.put(key, value);
        }
        @Override
        public V get(Object key) {
            V value = super.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + " does not contain value for " + key);
            }
            if (value instanceof Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
                        + " (try using the full name including the namespace, or rename one of the entries)");
            }
            return value;
        }

        private String getShortName(String key) {
            final String[] keyparts = key.split("\\.");
            return keyparts[keyparts.length - 1];
        }

        protected static class Ambiguity {
            private String subject;

            public Ambiguity(String subject) {
                this.subject = subject;
            }

            public String getSubject() {
                return subject;
            }
        }
    }
}
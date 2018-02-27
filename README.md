
###########################Mybatis的XMl文件的热加载解决方案#########################
<br>
<h5>作者：尹晓晨<h5>
<h3>##前言：</h3>
Mybatis有注解、xml文件两种写sql语句的方式。在xml文件中可以写sql语句，方式更灵活，也更方便维护，但是这种方式有一个缺陷就是无法被热部署，即使强大的jrebel插件也没能解决。每当改写sql语句就要重启服务器令开发者头疼不已。

现根据网上的方案做了整合，改了其中的一些bug，亲测非常好用, 若满意请star下谢谢。
<h3>##解决办法如下：</h3>
首先将给出的3个公共文件放入你的项目中：<br>
1： MapperRefresh.java（刷新文件的工具类，修改其中的配置）；<br>

        /*初始化配置（按个人需要进行修改）*/
        private static boolean enabled=true;         // 是否启用Mapper刷新线程功能<br>
        private static int delaySeconds=1;        // 延迟刷新秒数<br>
        private static int sleepSeconds=1;        // 休眠时间<br>
        private static String mappingPath="dao";      // xml文件所在的文件夹名(不带路径)，需要根据需要修改<br>

2：SqlSessionFactoryBean（重写的SqlSessionFactoryBean，用来替换掉原来自带的）；<br>
3：google-collections-1.0.jar(google的jar包，MapperRefresh.java文件会用到) <br>
（若你的项目为maven项目： <br>

    <dependency>
      <groupId>com.google.code.google-collections</groupId>
      <artifactId>google-collect</artifactId>
      <version>snapshot-20080530</version>
    </dependency>
 
 ）   
  
其他的区别在于你的项目是SpringBoot+Mybatis还是SSM：
<h4>一、SpringBoot+Mybatis环境：</h4>
1：修改你的MyBatisConfig.java文件（即spring配置mybatis的文件），将原来的SqlSessionFactoryBean替换为给出的SqlSessionFactoryBean<br>
2：修改application.yml<br>
    
    mybatis:
        typeAliasesPackage: com.hand.**.model
        mapperLocations: classpath*:com/hand/**/sqlMap/*Mapper.xml
        configLocation: classpath:mybatis-config.xml
    
<h4>二、SSM环境：</h4>
1： 用重写的SqlSessionFactoryBean，用来替换掉原来自带的即可）：<br>
 <!-- 配置mybitas SqlSessionFactoryBean-->
    <bean id="sqlSessionFactory" class="com.maintainsys.util.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="mapperLocations" value="classpath*:/com/maintainsys/dao/*Mapper.xml"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
    </bean>

<h2>大功告成：</h4>
若成功，则修改xml文件后控制台会提示类似代码：<br>

    ===========================================================================================================
    - (71775 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:140) 需要被刷新的Mapper文件个数: 1个
    - (71789 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:303) refresh key:RepairMapper
    - (71792 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:303) refresh key:BaseResultMap
    - (71801 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:303) refresh key:insertSelective
    - (71803 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:303) refresh key:updateByPrimaryKeySelective
    - (71804 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:303) refresh key:updateByPrimaryKeyWithBLOBs
    Refresh file: dao\RepairMapper.xml
    - (71807 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:194) 成功刷新Mapper文件: E:\AllProjects\maintainsys\target\maintainsys\WEB-INF\classes\com\maintainsys\dao\RepairMapper.xml
    - (71807 ms) - 2018-2-27 14:08:14[DEBUG](MapperRefresh.java:195) 被刷新的文件名: RepairMapper.xml
    ===========================================================================================================

赶紧试试吧！
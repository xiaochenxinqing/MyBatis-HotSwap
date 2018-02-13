
#######################Mybatis的XMl文件的热加载解决方案#########################


##########Spring boot配置mybatis的xml资源热加载
首先整理出几个需要注意的文件：
1： MapperRefresh；
2： SqlSessionFactoryBean；
3：mybatis-refresh.properties（在resource目录下）；

4：application.yml（resource目录下）；
5：com.hand.core.mybatis.MyBatisConfig。
前面三个文件，是原来旧的spring+springmvc+mybatis架构的工程使用mybatis热加载的时候的三个文件，没有任何变化，然后下面两个文件，说一下MyBatisConfig，，这个是我新写的一个文件，这个文件的作用相当于以前的在applicationContext.xml文件中配置sqlSessionFactoryBean，现在是手动配置这个bean的来源；最后是yml文件，这个文件中只改了一行代码：
原来的：
mybatis:
  typeAliasesPackage: com.hand.**.model
  mapperLocations: classpath*:com/hand/**/sqlMap/*Mapper.xml
  config-location: mybatis-config.xml
现在的：
mybatis:
  typeAliasesPackage: com.hand.**.model
  mapperLocations: classpath*:com/hand/**/sqlMap/*Mapper.xml
configLocation: classpath:mybatis-config.xml

配置好这些文件之后，启动程序之后修改xml文件，日志就应该会出现这一句话：
 
这时候热加载就应该是成功了。


#########SSM框架配置mybatis的xml资源热加载
大体同上，唯一区别是
 
配置好这些文件之后，启动程序之后修改xml文件，日志就应该会出现这一句话：
 
这时候热加载就应该是成功了。


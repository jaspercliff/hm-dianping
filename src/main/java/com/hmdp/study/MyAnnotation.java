package com.hmdp.study;

import java.lang.annotation.*;

@Documented
// 表示该annotation可以出现在javadoc中
@Target({ElementType.TYPE,ElementType.FIELD})
//表示该注解只能使用在 class interface enum declaration field 不指定则是任意地方
@Retention(RetentionPolicy.RUNTIME)
//annotation的策略(保留)属性  将annotation的信息保存在.class文件中,并且可以被虚拟机读取
@Inherited
//表示标注的注解具有继承性
public @interface MyAnnotation {

}

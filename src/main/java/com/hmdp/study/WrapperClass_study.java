package com.hmdp.study;

public class WrapperClass_study {
    public static void main(String[] args) {
        Integer a = null;
//        Integer b = true? a:12;

        Integer c = 10;
        int d = c.intValue();
        System.out.println(d);

//        System.out.println(b);


        /**
         * 整形对象通过使用相同的对象引用实现了缓存和重用
         * 适用于整数值区间-128  到   +127
         * 只适用于自动装箱 适用构造函数创建时不适用
         *
         * jvm会自动维护八种基本类型的常量池，int常量池中初始化-128-127的范围，所以在INterger i= 127 时
         * 在自动装箱过程中是取自常量池中的数值
         */


        Integer integer1 = 3;
        Integer integer2 = 3;
        if (integer1 == integer2)
            System.out.println("integer1 == integer2");
        else
            System.out.println("integer1 != integer2");

        Integer integer3 = 300;
        Integer integer4 = 300;

        if (integer3 == integer4)
            System.out.println("integer3 == integer4");
        else
            System.out.println("integer3 != integer4");
    }
}

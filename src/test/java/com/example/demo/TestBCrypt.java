package com.example.demo;

import cn.hutool.crypto.digest.DigestUtil;

public class TestBCrypt {

    public static void main(String[] args) {

        // 生成123456的BCrypt密码
        String encode = DigestUtil.bcrypt("123456");

        System.out.println("生成的密码：");
        System.out.println(encode);

        System.out.println("长度：" + encode.length());

        System.out.println("校验结果：");
        System.out.println(DigestUtil.bcryptCheck("123456", encode));
    }
}
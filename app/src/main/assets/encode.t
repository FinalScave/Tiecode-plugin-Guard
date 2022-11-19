@全局类
类 __加解密类
    @静态
    方法 __解密(内容 : 文本) : 文本
        @code
        int len = #内容.length();
        char[] chars = new char[len];
        for (int i = 0;i < len;i++) {
            char ch = (char) (#内容.charAt(i) + len);
            chars[i] = ch;
        }
        return new String(chars);
        @end
    结束 方法
结束 类
@全局类
类 __加解密类

    @code
    private static boolean error = false;

    static {
        if (!(#<__加解密类>.class.getClassLoader() instanceof dalvik.system.PathClassLoader)) {
            error = true;
        }
    }
    @end

    @静态
    方法 __解密(内容 : 文本) : 文本
        @code
        if (error) { return "??????"; }
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
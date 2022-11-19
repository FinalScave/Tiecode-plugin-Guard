@全局类
类 __加解密类
    @静态
    方法 __解密(内容 : 文本) : 文本
        @code
        var len = #内容.length;
        var chars = [];
        for (var i = 0;i < len;i++) {
            var ch = char(#内容.charAt(i) + len);
            chars.push(ch);
        }
        return String(chars);
        @end
    结束 方法
结束 类
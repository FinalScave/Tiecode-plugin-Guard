包名 结绳.加密器

@全局类
类 __加解密类

    @code
    private static boolean error = false;

    static {
        if (!(#<__加解密类>.class.getClassLoader() instanceof dalvik.system.PathClassLoader)) {
            error = true;
        }
    }

    private final static String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    @end

    @静态
    方法 __解密(内容 : 文本) 为 文本
		@code
		if (error) { return "??????"; }
		String base64Binarys = "";
		#内容 = #内容.substring(0, #内容.length() - 2);
		for (int i = 0; i < #内容.length(); i++) {
			char s = #内容.charAt(i);
			if (s != '=') {
				String binary = Integer.toBinaryString(CHARS.indexOf(s));
				while (binary.length() != 6) {
					binary = "0" + binary;
				}
				base64Binarys += binary;
			}
		}
		base64Binarys = base64Binarys.substring(0, base64Binarys.length() - base64Binarys.length() % 8);
		byte[] bytesStr = new byte[base64Binarys.length() / 8];
		for (int bytesIndex = 0; bytesIndex < base64Binarys.length() / 8; bytesIndex++) {
			bytesStr[bytesIndex] = (byte) Integer.parseInt(base64Binarys.substring(bytesIndex * 8, bytesIndex * 8 + 8), 2);
		}
		return new String(bytesStr);
		@end
	结束 方法

结束 类
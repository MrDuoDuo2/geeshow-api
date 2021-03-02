# geeweshow-api

## 使用限制
    在您使用之前，您需要准备一个Token(登陆时，由GITHUB提供)，在访问每一个接口的时候，您需要传入您的UserID，和由Token加密生成的签名，获取签名方法请查看鉴权规则。


## 鉴权规则
### 背景信息
默认情况下，所有的API都需要验证客户的权限是否能访问当前API。所以我们提供两种验证权限的方式。

### 权限验证
### 通过Signature方式验证权限
````
  客户访问API时通过参数的方式将签名+签名方法+签名随机数+时间戳传入，后台判断该签名是否与服务器端签名一致。
````
### 签名机制
对于未携带Token的请求，我们将会根据访问中的签名信息验证访问请求者的权限。具体由使用UserID和Token对称加密验证实现。

#### 步骤一：构造规范化请求字符串
1、排序参数。排序规则以首字母顺序排序，排序参数包括公共请求参数和接口自定义参数，不包括公共请求参数中的Signature参数。

2、编码参数。使用UTF-8字符集编码请求参数和参数取值，编码规则如下：

字符A ~ Z、a ~ z、0 ~ 9以及字符-、_、.、~不编码。

其它字符编码成%XY的格式，其中XY是字符对应ASCII码的16进制。示例：半角双引号（"）对应%22。

扩展的UTF-8字符，编码成%XY%ZA…的格式。

空格（ ）编码成%20，而不是加号（ + ）。 该编码方式与application/x-www-form-urlencodedMIME格式编码算法相似，但又有所不同。 如果您使用的是Java标准库中的java.net.URLEncoder，可以先用标准库中percentEncode编码，随后将编码后的字符中加号（+）替换为%20、星号（*）替换为%2A、%7E替换为波浪号（~），即可得到上述规则描述的编码字符串。

3、使用等号（=）连接编码后的请求参数和参数取值。

4、使用与号（&）连接编码后的请求参数，注意参数排序与步骤1一致。

#### 步骤二：构造签名字符串
1、构造待签名字符串StringToSign。您可以同样使用percentEncode处理上一步构造的规范化请求字符串，规则如下：

StringToSign=
  HTTPMethod + "&" + //HTTPMethod：发送请求的 HTTP 方法，例如 GET。
  percentEncode("/") + "&" + //percentEncode("/")：字符（/）UTF-8 编码得到的值，即 %2F。
  percentEncode(CanonicalizedQueryString) //您的规范化请求字符串。
2、计算待签名字符串

  Mac mac = Mac.getInstance(algorithm);//algorithm为选择的加密方法
  mac.init(new SecretKeySpec(accessKeySecret.getBytes(ENCODING), algorithm));
  byte[] signData = mac.doFinal(String.valueOf(stringToSign).getBytes(ENCODING));

  Signature = Base64.getEncoder().encodeToString(signData);
#### 示例一：参数拼接法
以调用GetToken为例，假设您获得了AccessKeyID(UserId)=testid以及AccessKeySecret(Token)=testsecret，签名流程如下所示：

1、构造规范化请求字符串

http://192.168.2.43:8080/check?SignatureNonce=5c5c9b47-387e-4e5e-afa3-423d16c86d9c&UserId=45281356&SignatureMethod=HmacSHA1&Timestamp=2021-03-02%2017%3A51%3A43.61

2、构造待签名字符串StringToSign

GET&%2F&SignatureMethod%3DHmacSHA1%26SignatureNonce%3D5c5c9b47-387e-4e5e-afa3-423d16c86d9c%26Timestamp%3D2021-03-02%252017%253A51%253A43.61%26UserId%3D45281356

3、计算签名值，得到t5vguL43pJiZeVUzQn0i8EHwr2M=（当前使用的是HMAC-SHA1）

Signature = Base64( HMAC-SHA1( AccessKeySecret, UTF-8-Encoding-Of(StringToSign) ) )
4、将得到的Signature加入步骤一的URL，即可得到完整的URL链接

http://192.168.2.43:8080/check?SignatureNonce=5c5c9b47-387e-4e5e-afa3-423d16c86d9c&UserId=45281356&Signature=cTeyURZ7fu%2FKDw7rhCv3lH0fymM%3D&SignatureMethod=HmacSHA1&Timestamp=2021-03-02%2017%3A51%3A43.61
#### 示例二：编程语言法
以调用GetToken为例，假设您获得了AccessKeyID=testid以及AccessKeySecret=testsecret，签名流程如下所示：

1、预定义编码方法

  public static String percentEncode(String value) throws UnsupportedEncodingException {
      return value != null ? URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~") : null;
  }
2、构造请求字符串

  // 排序请求参数
  String[] sortedParams = params.keySet().toArray(new String[]{});
  Arrays.sort(sortedParams);

  final String HTTP_METHOD = "GET";
  final String SEPARATOR = "&";

  // 构造用于签名的字符串
  StringBuilder stringToSign = new StringBuilder();

  stringToSign.append(HTTP_METHOD).append(SEPARATOR);
  tlog.var("stringToSign", stringToSign);

  stringToSign.append(ThclUrlUtil.percentEncode("/")).append(SEPARATOR);
  tlog.var("stringToSign", stringToSign);

  StringBuilder canonicalizedQueryString = new StringBuilder();

  for (String param : sortedParams) {
      // 构造查询参数（如&Timestamp=xxx），并追加到canonicalizedQueryString最后
      canonicalizedQueryString.append("&")
              .append(ThclUrlUtil.percentEncode(param)).append("=")
              .append(ThclUrlUtil.percentEncode((String) params.get(param)));
      tlog.var("canonicalizedQueryString", canonicalizedQueryString);
  }

  // 构造用于签名的字符串：URL编码后的查询字符串
  stringToSign.append(ThclUrlUtil.percentEncode(
          canonicalizedQueryString.toString().substring(1)));
3、签名

  Mac mac = Mac.getInstance(algorithm);//algorithm为选择的加密方法
  mac.init(new SecretKeySpec(accessKeySecret.getBytes(ENCODING), algorithm));
  byte[] signData = mac.doFinal(String.valueOf(stringToSign).getBytes(ENCODING));

  Signature = Base64.getEncoder().encodeToString(signData);
4、将得到的Signature加入步骤一的URL，即可得到完整的URL链接

http://192.168.2.43:8080/check?SignatureNonce=5c5c9b47-387e-4e5e-afa3-423d16c86d9c&UserId=45281356&Signature=cTeyURZ7fu%2FKDw7rhCv3lH0fymM%3D&SignatureMethod=HmacSHA1&Timestamp=2021-03-02%2017%3A51%3A43.61


## 数字服务接口

### /
主页检测登陆

### Url
#### url
http://192.168.2.43:8080/

#### 请求方法
GET

### callback
接受授权请求后，获取的Github的返回值
### Url
#### url
http://192.168.2.43:8080/callback?code=d31154d1f47d3fb4bfff&state=1

#### 请求方法
GET

#### 请求参数
| 参数  | 名称 | 示例值 |
| :-----| ----: | :----: |
| code | Github返回的代码 | d31154d1f47d3fb4bfff|
| state | 请求Github时携带的随即数 | 1 |

### check
用户第二次访问的校验

### URL
#### Url	
http://192.168.2.43:8080/check?SignatureNonce=5c5c9b47-387e-4e5e-afa3-423d16c86d9c&UserId=45281356&Signature=cTeyURZ7fu%2FKDw7rhCv3lH0fymM%3D&SignatureMethod=HmacSHA1&Timestamp=2021-03-02%2017%3A51%3A43.61

#### 请求方式	
Get

#### 参数
| 参数  | 名称 | 示例值 |
| :-----| ----: | :----: |
| UserId | 用户ID | 45281356 |
| Signature | 数字签名 | cTeyURZ7fu%2FKDw7rhCv3lH0fymM%3 |
| SignatureNonce | 签名唯一随机数。用于防止网络重放攻击，建议您每一次请求都使用不同的随机数。 | 5c5c9b47-387e-4e5e-afa3-423d16c86d9c|
| SignatureMethod | 签名方式。取值范围：HMAC-SHA1。 | HmacSHA1 |
| Timestamp | 时间戳 | 2021-03-02%2017%3A51%3A43.61 |



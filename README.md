
# StringFog
一款自动对dex/aar/jar文件中的字符串进行加密Android插件工具，正如名字所言，给字符串加上一层雾霭，使人难以窥视其真面目。

[![JitPack](https://img.shields.io/jitpack/v/github/mobcoding/StringFog.svg?label=JitPack)](https://jitpack.io/#mobcoding/StringFog)
[![License](https://img.shields.io/badge/license-Apache2.0-brightgreen)](LICENSE)
[![AGP](https://img.shields.io/badge/AGP-8%20%2F%209-blue)](https://developer.android.com/build/releases/gradle-plugin)

> 原项目由 MegatronKing 开源并维护。本 fork 维护于 [mobcoding/StringFog](https://github.com/mobcoding/StringFog)，兼容 AGP 8/9，并通过 JitPack 发布。

- 支持java/kotlin。
- 支持app打包生成的apk加密。
- 支持aar和jar等库文件加密。
- 支持加解密算法的自主扩展。
- 支持配置可选代码加密。
- 完全Gradle自动化集成。
- 不支持InstantRun。

**维护说明**
> 本仓库基于 [MegatronKing/StringFog](https://github.com/MegatronKing/StringFog) 维护，保留原项目的 Apache-2.0 许可证和版权声明。

当前 fork 同时维护 AGP 8 和 AGP 9 兼容版本，并支持在最终 app 模块中选择性加密外部 AAR/JAR 的 class。欢迎通过 Issue 或 PR 提交问题与改进。

### 原理

![](https://github.com/mobcoding/StringFog/blob/master/assets/flow.png)<br>

- 加密前：
```java
String a = "This is a string!";
```

- 加密后：
```java
String a = StringFog.decrypt(new byte[]{-113, 71...}, new byte[]{-23, 53});

```

- 运行时：
```java
decrypt: new byte[]{-113, 71...} => "This is a string!"
```

### 混淆
StringFog 与 R8/ProGuard 不冲突，也不需要为插件自动生成的解密类手动添加 `-keep` 规则。构建时插件会生成 `<应用包名>.StringFog`，加密后的字节码会直接引用它；混淆时 R8 会同步更新类定义和所有引用。

不要手动创建、复制或删除这个生成类。若使用了自定义算法实现，仍需将实现类和其依赖作为应用运行时依赖保留在最终 app 中。

### 使用
由于开发了 Gradle 插件，所以在集成时非常简单，不会影响到打包的配置。本 fork 通过 JitPack 发布，最新版本为 `5.3.3`。

#### 版本兼容性

StringFog 版本必须与消费工程的 Android Gradle Plugin（AGP）版本匹配，不能只按最新版本升级：

| 消费工程 AGP | StringFog 版本 | Gradle / JDK | 外部 AAR/JAR 加密 |
|---|---|---|---|
| AGP 8.x | `5.2.2` | Gradle 8.x / JDK 17 | 支持 |
| AGP 9.x | `5.3.3` | Gradle 9.x / JDK 17 | 支持 |

- `5.2.2` 是包含外部 AAR/JAR 加密能力的 AGP 8 兼容版本。
- `5.3.3` 基于 AGP 9 API 发布，不应直接用于 AGP 8 工程。
- `gradle-plugin` 与 `xor` 必须使用相同版本。下方示例以 AGP 9 的 `5.3.3` 为例；AGP 8 工程请将所有 `5.3.3` 统一替换为 `5.2.2`。

##### 1、在根目录build.gradle中引入插件依赖。
JitPack 坐标使用上表中与 AGP 匹配的版本号（`5.3.3` 或 `5.2.2`），不要添加 Git tag 的 `v` 前缀。

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        ...
        classpath 'com.github.mobcoding.StringFog:gradle-plugin:5.3.3'
        // 选用加解密算法库，默认实现了xor算法，也可以使用自己的加解密库。
        classpath 'com.github.mobcoding.StringFog:xor:5.3.3'
    }
}
```

根工程使用 `build.gradle.kts` 时，等价配置如下：

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
    dependencies {
        classpath("com.github.mobcoding.StringFog:gradle-plugin:5.3.3")
        classpath("com.github.mobcoding.StringFog:xor:5.3.3")
    }
}
```

##### 2、在app或lib的build.gradle中配置插件。
```groovy
// 导入RandomKeyGenerator类，如果使用HardCodeKeyGenerator，更换下类名
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import com.github.megatronking.stringfog.plugin.StringFogMode

apply plugin: 'stringfog'

stringfog {
    // 必要：加解密库的实现类路径，需和上面配置的加解密算法库一致。
    implementation 'com.github.megatronking.stringfog.xor.StringFogImpl'
    // 可选：StringFog会自动尝试获取packageName，如果遇到获取失败的情况，可以显式地指定。
    packageName 'com.github.megatronking.stringfog.app'
    // 可选：加密开关，默认开启。
    enable true
    // 可选：指定需加密的代码包路径，可配置多个；未指定时默认加密当前模块的全部代码。
    // 在最终 app 模块配置非空包名时，匹配包名下外部 AAR/JAR 的 class 也会被加密。
    // 填写包名，不要带类名或末尾的 "."；资源、assets 和 so 文件不在处理范围内。
    fogPackages = ['com.xxx.xxx']
    // 可选（3.0版本新增）：指定密钥生成器，默认使用长度8的随机密钥（每个字符串均有不同随机密钥）,
    // 也可以指定一个固定的密钥：HardCodeKeyGenerator("This is a key")
    kg new RandomKeyGenerator()
    // 可选（4.0版本新增）：用于控制字符串加密后在字节码中的存在形式，默认为base64，
    // 也可以使用bytes
    mode StringFogMode.base64
}
```

模块使用 `build.gradle.kts` 时，等价配置如下：
```kotlin
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator

// Use the buildscript classpath from the root project, then apply the plugin here.
apply(plugin = "stringfog")

configure<StringFogExtension> {
    // 必要：加解密库的实现类路径，需和上面配置的加解密算法库一致。
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    // 可选：加密开关，默认开启。
    enable = true
    // 可选：指定需加密的代码包路径，可配置多个；未指定时默认加密当前模块的全部代码。
    // 在最终 app 模块配置非空包名时，匹配包名下外部 AAR/JAR 的 class 也会被加密。
    fogPackages = arrayOf("com.xxx.xxx")
    kg = RandomKeyGenerator()
    // base64或者bytes
    mode = StringFogMode.bytes
}
```

##### 3、在app或lib的build.gradle中引入加解密库依赖。

```groovy
dependencies {
      ...
      // 这里要和上面选用的加解密算法库一致，用于运行时解密。
      implementation 'com.github.mobcoding.StringFog:xor:5.3.3'
}
```

Kotlin DSL：

```kotlin
dependencies {
    implementation("com.github.mobcoding.StringFog:xor:5.3.3")
}
```

#### 加密外部 AAR/JAR
在**最终 application 模块**中配置非空的 `fogPackages` 后，插件会处理依赖 AAR/JAR 中匹配包名的 `.class` 文件。例如，第三方 AAR 的实现包为 `com.example.secure`：

```groovy
stringfog {
    implementation 'com.github.megatronking.stringfog.xor.StringFogImpl'
    fogPackages = ['com.example.secure']
}
```

- `fogPackages` 按包名边界匹配，可配置多个。`com.example.foo` 会匹配 `com.example.foo.*`，不会匹配 `com.example.foobar.*`；空字符串会在构建时直接报错。
- 只会处理 `fogPackages` 选中的 class。未选中的依赖、资源、assets 和 native `.so` 不会被加密。
- `fogPackages` 为空时，保持原有行为：仅处理当前模块的 class，不会处理外部 AAR/JAR。
- Android library 和 dynamic-feature 模块始终只处理本模块 class；要加密外部依赖，请在最终 app 模块应用插件并配置 `fogPackages`。
- 解密入口 `StringFog` 由插件在构建时自动生成。其包名依次取自 Manifest 的 `package`、Android `namespace`、`stringfog.packageName`。外部 AAR 被加密后会调用该入口，因此最终 app 必须保留上方的 xor（或自定义算法）运行时依赖。

`5.2.2` 基于 AGP `8.0.0` API 发布；`5.3.3` 基于 AGP `9.0.0` API 发布，JitPack 构建使用 Gradle `9.5` 和 JDK `17`。

##### 注意事项
- AGP 8 工程使用 StringFog `5.2.2` 时，需要开启 `android.buildFeatures.buildConfig = true`。
- StringFog `5.3.3` 不依赖 `BuildConfig`，无需仅为 StringFog 开启该配置。

### 扩展

#### 注解反加密
如果开发者有不需要自动加密的类，可以使用注解StringFogIgnore来忽略：
```java
import com.github.megatronking.stringfog.annotation.StringFogIgnore;

@StringFogIgnore
public class Test {
    ...
}
```
#### 自定义加解密算法实现
实现IStringFog接口，参考stringfog-ext目录下面的xor算法实现。
注意某些算法在不同平台上会有差异，可能出现在运行时无法正确解密的问题。如何集成请参考下方范例！
```java
public final class StringFogImpl implements IStringFog {

    @Override
    public byte[] encrypt(String data, byte[] key) {
        // 自定义加密
    }

    @Override
    public String decrypt(byte[] data, byte[] key) {
        // 自定义解密
    }

    @Override
    public boolean shouldFog(String data) {
        // 控制指定字符串是否加密
        // 建议过滤掉不重要或者过长的字符串
        return true;
    }

}

```

#### 自定义密钥生成器
实现IKeyGenerator接口，参考RandomKeyGenerator的实现。

#### Mapping文件
**注意⚠️：StringFog 5.x版本起有问题，已暂时停用此功能**
加解密的字符串明文和暗文会自动生成mapping映射文件，位于outputs/mapping/stringfog.txt。

## 范例
- 默认加解密算法集成，可参考上游 [sample1](https://github.com/MegatronKing/StringFog-Sample1)；请将依赖坐标替换为本文的 JitPack 坐标。
- 自定义加解密算法集成，可参考上游 [sample2](https://github.com/MegatronKing/StringFog-Sample2)；请将依赖坐标替换为本文的 JitPack 坐标。

## 更新日志

### v5.3.3
- 支持在最终 app 模块中通过非空 `fogPackages` 加密外部 AAR/JAR 的匹配 class；未配置时保持仅加密当前模块的原有行为。
- 升级为基于 AGP 9 Instrumentation API 的实现，library 模块保持仅处理本模块 class。
- 插件不再发布 AGP API 运行时依赖，避免污染消费工程的构建 classpath。

### v5.2.2
- 为 AGP 8 工程支持在最终 app 模块中通过非空 `fogPackages` 加密外部 AAR/JAR 的匹配 class。
- 插件不再发布 AGP API 运行时依赖，避免污染消费工程的构建 classpath。

### v5.2.0
- 从ASM7升级到ASM9。
- 修复多模块配置问题。

### v5.1.0
- 修复获取无法获取packageName的问题。
- 修复无法指定KeyGenerator的问题。
- 优化生成StringFog.java文件的任务逻辑。
- 暂时移除Mapping文件生成逻辑，可能导致无法删除的问题。

### v5.0.0
- 支持Gradle 8.0。

### v4.0.1
- 修复Base64 API版本兼容问题。

### v4.0.0
- 使用ASM7以支持Android 12。
- 支持AGP(Android Gradle Plugin) 7.x版本。
- DSL新增StringFogMode选项，用于控制字符串加密后在字节码中的存在形式，支持base64和bytes两种模式，默认使用base64。
    - base64模式：将字符串加密后的字节序列使用base64编码，行为同1.x和2.x版本。
    - bytes模式：将字符串加密后的字节序列直接呈现在字节码中，行为同3.x版本。

### v3.0.0
- 密文不再以String形式存在，改为直接字节数组，感谢PR #50。
- 重构公开API相关代码（不兼容历史版本）。
- 删除AES加密实现，考虑到存在bug和性能问题且意义不大。
- xor算法移除base64编码。
- 固定加密字符串key改为随机key，且提供IKeyGenerator接口支持自定义实现。
- 插件依赖的ASM库由5.x升级到9.2。

### v2.2.1
- 修复module-info类导致的报错问题

### v2.2.0
- 支持AGP(Android Gradle Plugin) 3.3.0+版本

### v2.1.0
- 修复kotlin打包的bug

### v2.0.1
- 增加implementation自定义算法实现类详细报错信息

### v2.0.0
- 修改gradle配置（必须配置implementation指定算法实现）。
- 修复大字符串编译失败的问题。
- 新增自定义加解密算法扩展。
- 新增生成mapping映射表文件。

### v1.4.1
- 修复使用Java 8时出现的ZipException编译错误

### v1.4.0
- 新增指定包名加密的配置项：fogPackages
- 移除指定包名不加密的配置项：exclude

### v1.3.0
- 修复gradle 3.0+编译报错的bug

### v1.2.2
- 修复windows下打包后报错的bug

### v1.2.1
- 修复windows下文件分隔符的bug
- 修复applicationId和packageName不一致导致无法编译的bug
- 优化功能，不需要再手动exclude已使用StringFog的库

### v1.2.0
- 支持在library中使用，每个library可以使用不同key
- 支持exclude指定包名不进行加密
- 修复一些已知bug


--------

    Copyright (C) 2016-2023, Megatron King

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

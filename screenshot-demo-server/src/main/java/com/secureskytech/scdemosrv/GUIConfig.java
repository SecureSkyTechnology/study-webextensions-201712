package com.secureskytech.scdemosrv;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Files;
import com.secureskytech.scdemosrv.proxy.LFSMapEntry;
import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

import lombok.Data;

/**
 * YAMLを使ったGUI設定の保存クラス
 * 
 * なぜYAMLを使うのか？ Swingコンポーネントならserializeableなので、Java標準のJNLPのPersistenceServiceなどを使えばよかったのでは？
 * 
 * => パスワードや close 時のJFrameの位置・サイズなど、単純にserializeしては地雷が潜んでいる可能性があり、
 * 地雷除去できるだけのスキルが2017-03時点では坂本が持っていなかった。
 * また、JNLPのPersistenceServiceでは、Windowsの場合の保存場所がAppDataの下などかなり深くなってしまい、
 * ユーザによる設定値クリアなどのトラブル時の運用ガイドが難しくなる可能性を感じた。
 * （同様の理由で、レジストリに保存する Preferences API も却下した）
 * その上で、シンプルなBeansで簡単にserialize/desirializeできて安定して動作するYAMLを用いることで、
 * コード全体を簡素化した。
 * 
 * もう一つの考慮事項として、今後の機能追加でGUI設定として保存する項目が増える場合がある。
 * その際、単純に「エラーになったらこのファイル削除して再起動して」と案内できるだけの
 * 単純さを実現できる方式を優先したかった。
 * (serializeにおけるフィールド追加による差分の扱いがもう一つの地雷だった）
 *  
 * 参考：
 * @see http://ateraimemo.com/Swing/Preferences.html
 * @see http://ateraimemo.com/Swing/PersistenceService.html
 */
@Data
public class GUIConfig {
    public static final File DEFAULT_CONFIG_FILE =
        new File(System.getProperty("user.home") + "/.screenshot-demo-server.yml");
    public static final int DEFAULT_PROXY_LISTENING_PORT = 10088;
    public static final int DEFAULT_WEBUI_LISTENING_PORT = 10089;

    int proxyPort = DEFAULT_PROXY_LISTENING_PORT;
    int webUIPort = DEFAULT_WEBUI_LISTENING_PORT;
    List<LFSMapEntry4Yaml> listOfMapEntry4Yaml = new ArrayList<>();
    List<String> targetHostNames = Arrays.asList("*");
    List<String> excludeFilenameExtensions = Arrays.asList("js", "gif", "jpg", "png", "css", "bmp", "svg", "ico");

    public static GUIConfig load(File f) throws IOException {
        Yaml y = new Yaml();
        return y.loadAs(Files.toString(f, StandardCharsets.UTF_8), GUIConfig.class);
    }

    public void save(File f) throws IOException {
        Yaml y = new Yaml();
        Files.write(y.dump(this), f, StandardCharsets.UTF_8);
    }

    public static void initIfDefaultConfigFileNotExists() throws IOException {
        if (!DEFAULT_CONFIG_FILE.exists()) {
            GUIConfig defc = new GUIConfig();
            defc.save(GUIConfig.DEFAULT_CONFIG_FILE);
        }
    }

    public static String convertWildcardToRegexp(String s) {
        s = s.replace(".", "\\.");
        s = s.replace("*", ".*");
        s = "^" + s + "$";
        return s;
    }

    public static boolean isValidRegexpPattern(String p) {
        if (null == p) {
            return false;
        }
        try {
            Pattern.compile(p);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * getにするとSnakeYamlに見つかってしまうので悔しまぎれの "convertTo".
     * 
     * @return
     */
    public List<LFSMapEntry> convertToLFSMapEntries() {
        List<LFSMapEntry> r = new ArrayList<>(listOfMapEntry4Yaml.size());
        for (LFSMapEntry4Yaml m : listOfMapEntry4Yaml) {
            r.add(m.toLFSMapEntry());
        }
        return r;
    }

    /**
     * setにするとSnakeYamlに見つかってしまうので悔しまぎれの "update".
     * 
     * @param maps
     */
    public void updateMapEntries(List<LFSMapEntry> maps) {
        this.listOfMapEntry4Yaml.clear();
        for (LFSMapEntry m : maps) {
            this.listOfMapEntry4Yaml.add(new LFSMapEntry4Yaml(m));
        }
    }

    /**
     * SnakeYamlで、Bean内のFileやCharsetまでserialize/deserializeする方法がすぐ見つからなかったので、
     * やむなく SnakeYaml で確実にconvertできる範囲の型に押さえた中間型を導入。
     */
    @Data
    public static class LFSMapEntry4Yaml {
        String hostHeader;
        String path;
        String localDir;
        IndexHandlePolicy indexHandlePolicy;
        Set<String> mappedExtensions;
        String textCharset;

        public LFSMapEntry4Yaml() {
            // empty constructor for SnakeYaml deserialization.
        }

        public LFSMapEntry4Yaml(LFSMapEntry src) {
            hostHeader = src.getHostHeader();
            path = src.getPath();
            localDir = src.getLocalDir().toString();
            indexHandlePolicy = src.getIndexHandlePolicy();
            mappedExtensions = src.getMappedExtensions();
            textCharset = src.getTextCharset().name();
        }

        public LFSMapEntry toLFSMapEntry() {
            return new LFSMapEntry(
                this.hostHeader,
                this.path,
                this.localDir,
                this.indexHandlePolicy,
                this.mappedExtensions,
                Charset.forName(this.textCharset));
        }
    }
}

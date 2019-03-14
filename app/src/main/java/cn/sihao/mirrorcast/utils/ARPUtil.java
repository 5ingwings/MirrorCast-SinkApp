package cn.sihao.mirrorcast.utils;



import com.orhanobut.logger.Logger;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * 通过arp获取source端 wifi p2p设备mac地址对应的ip地址
 * reference:https://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wi-fi-direct-scenario
 */
public class ARPUtil {
    private final static String TAG = "MiraARPUtil";

    public static String getIPFromMac(String macAddr) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            while ((line = br.readLine()) != null) {
                Logger.t(TAG).d("arp table line is:" + line);
                if (line.contains("p2p0")) { // 若包括p2p0并且截取的MAC addr 等于 source端设备的mac addr
                    String tmp = line.replace(" ", "");
                    int starPos = tmp.indexOf("*");
                    String deviceMac = tmp.substring(starPos - 17, starPos);
                    Logger.t(TAG).d("arp table deviceMac:" + deviceMac + ", connect info source macAddr:" + macAddr);
                    if (deviceMac.equals(macAddr)) {
                        return line.substring(0, line.indexOf(" "));
                    }
                }
            }
        } catch (Exception e) {
            Logger.t(TAG).e("getIPFromMac() Exception:" + e.toString());
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                Logger.t(TAG).e("getIPFromMac() Exception:" + e.toString());
            }
        }
        return "";
    }

}

package jp.mcapps.android.multi_payment_terminal.thread.emv.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Properties;

import timber.log.Timber;

/**
 * Assets文件工具类
 */
public class AssetsUtil {

    private static WeakReference<Context> mContextRef;

    public static void init(Context context) {
        mContextRef = new WeakReference<>(context);
    }

    private static AssetManager getAssetManager() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return context.getResources().getAssets();
    }

    private static Context getContext() {
        if (mContextRef == null) {
            throw new IllegalStateException("You must init AssetsUtil first.");
        }
        return mContextRef.get();
    }


    /**
     * 获取Assets配置信息
     *
     * @param fileName 配置文件名
     * @return Properties
     */
    public static Properties lodeConfig(String fileName) {
        Properties prop = new Properties();
        AssetManager assetManager = getAssetManager();
        if (assetManager == null) {
            return null;
        }
        try {
            prop.load(assetManager.open(fileName));
        } catch (IOException e) {
            Timber.d("load property file fail.");
            e.printStackTrace();
            return null;
        }
        return prop;
    }

    /**
     * 根据配置文件名和配置属性获取值
     *
     * @param fileName 配置文件名
     * @param name     配置的属性名
     * @return 对应文件对应属性的String
     */
    public static String lodeConfig(String fileName, String name) {
        Properties pro = lodeConfig(fileName);
        if (pro == null) {
            return null;
        }
        return (String) pro.get(name);
    }



    /**
     * 将assets下文件拷贝至本应用程序data下
     *
     * @param fileName assets文件名称
     */
    public static boolean copyAssetsToData(String fileName) {
        Context context = getContext();
        try {
            AssetManager as = context.getAssets();
            InputStream ins = as.open(fileName);
//            String dstFilePath = context.getFilesDir().getAbsolutePath() + "/" + fileName;
            OutputStream outs = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            byte[] data = new byte[1 << 20];
            int length = ins.read(data);
            outs.write(data, 0, length);
            ins.close();
            outs.flush();
            outs.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static byte[]getAssertFileData(String fileName){
        Context context = getContext();
        try {
            AssetManager as = context.getAssets();
            InputStream ins = as.open(fileName);
            byte[] data = new byte[1 << 20];
            int length = ins.read(data);
            ins.close();
            byte[] out = new byte[length];
            System.arraycopy(data,0,out,0,length);
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 获取bundle参数列表
     */
    public static String getProps(String name, String proName) {
        Properties pro = new Properties();
        AssetManager assetManager = getAssetManager();
        if (assetManager == null) {
            return null;
        }
        try {
            pro.load(assetManager.open(name));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return null;
        }
        String prop = pro.getProperty(proName);
        if (prop == null)
            return null;
        try {
            prop = new String(prop.trim().getBytes("ISO-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return prop;
    }


    public static String[] getProParam(String filePath,String[] paramIndex){
        int ParamNum= paramIndex.length;
        int index = 0;
        String[] Params =new String[ParamNum];
        for (int i=0;i<ParamNum;i++){
            String temp = getProps(filePath,paramIndex[i]);
            if (temp!=null) {
                Params[i-index] = temp;
            }
            else {
                index++;
                Timber.d("Can't find " + paramIndex[i]);
            }
        }
        return Params;
    }


    /**
     * transaction status getting
     * @param id key of getting status.
     *  @string return the status string.
     * */
    public static String getStatus(int id,int language){
//        String[] pro = null;
//        pro = AssetsUtil.getPropsLan(AppParam.ENLAN,String.valueOf(id));
//        if (pro != null){
//            return pro[0];
//        }
        return null;
    }



    /**
     *get bundle
     */
    public static String[] getPropsLan(String name, String proName) {
        Properties pro = new Properties();
        AssetManager assetManager = getAssetManager();
        if (assetManager == null) {
            return null;
        }
        try {
            pro.load(assetManager.open(name));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return null;
        }
        String prop = pro.getProperty(proName);
        if (prop == null)
            return null;
        String[] results = prop.split(",");
        for (int i = 0; i < results.length; i++) {
            try {
                results[i] = new String(results[i].trim().getBytes("ISO-8859-1"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return results;
    }


}

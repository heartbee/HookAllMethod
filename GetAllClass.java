package com.example.yixianglin.hookallmethod;

import android.util.Log;

import java.lang.reflect.Array;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by yixianglin on 2018/3/14.
 */

public class GetAllClass {
    public static String[] getAllClass(ClassLoader classLoader){
        String[] classNames=null;
        Log.e("fuck","classloader----->"+classLoader.getClass().getName());
        try{
            Object pathList = XposedHelpers.getObjectField(classLoader, "pathList");//获取DexPathList对象
            Object dexElements = XposedHelpers.getObjectField(pathList, "dexElements");//获取DexPathList中的dexElements
            int dexElementsLength = Array.getLength(dexElements);

            for (int i = 0; i < dexElementsLength; i++) {
                Object dexElement = Array.get(dexElements, i);
                Object dexFile = XposedHelpers.getObjectField(dexElement, "dexFile");//获取DexFile对象
                Object cookie = XposedHelpers.getObjectField(dexFile, "mCookie");//获取mCookie
                String dexFileName = (String) XposedHelpers.getObjectField(dexFile, "mFileName");

                Log.d("fuck", String.format("DexFile: %s, %08x", dexFileName, cookie));

                classNames = (String[]) XposedHelpers.callMethod(dexFile, "getClassNameList", cookie);

            }
            return classNames;
        }catch (Exception e){
            e.printStackTrace();
            return null;

        }
    }
}

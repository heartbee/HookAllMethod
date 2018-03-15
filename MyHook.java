package com.example.yixianglin.hookallmethod;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by yixianglin on 2018/3/14.
 */

public class MyHook implements IXposedHookLoadPackage{
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        HookAllMethods.hookMethod(loadPackageParam);


    }
}

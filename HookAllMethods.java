package com.example.yixianglin.hookallmethod;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by yixianglin on 2018/3/14.
 */

public class HookAllMethods {
    public static Set<String> methodSignSet = Collections.synchronizedSet(new HashSet<String>());
    public static Set<String> callMethodSignSet = Collections.synchronizedSet(new HashSet<String>()) ;

    private static final String FILTER_PKGNAME = "com.jd.jrapp";

    public static void hookMethod(XC_LoadPackage.LoadPackageParam loadPackageParam){
        String pkgname = loadPackageParam.packageName;
        if(FILTER_PKGNAME.equals(pkgname)){
            //这里是为了解决app多dex进行hook的问题，Xposed默认是hook主dex
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ClassLoader cl = ((Context)param.args[0]).getClassLoader();
                    hooker(cl);


                    /*Class<?> hookclass = null;
                    try {
                        hookclass = cl.loadClass("dalvik.system.DexFile");
                    } catch (Exception e) {
                        return;
                    }

                    XposedHelpers.findAndHookMethod(hookclass, "loadClass", String.class, ClassLoader.class, new XC_MethodHook(){
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            hookClassInfo((String)param.args[0], (ClassLoader)param.args[1]);
                            super.beforeHookedMethod(param);
                        }
                    });

                    XposedHelpers.findAndHookMethod(hookclass, "loadClassBinaryName", String.class, ClassLoader.class, List.class,new XC_MethodHook(){
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            hookClassInfo((String)param.args[0], (ClassLoader)param.args[1]);
                            super.beforeHookedMethod(param);
                        }
                    });

                    XposedHelpers.findAndHookMethod(hookclass, "defineClass", String.class, ClassLoader.class, long.class, List.class,new XC_MethodHook(){
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            hookClassInfo((String)param.args[0], (ClassLoader)param.args[1]);
                            super.beforeHookedMethod(param);
                        }
                    });*/

                }
            });
        }
    }


    public static void hookClassInfo(String className, ClassLoader classLoader){
        //过滤系统类名前缀
        if(TextUtils.isEmpty(className)){
            return;
        }
        if(className.startsWith("android.")){
            return;
        }
        if(className.startsWith("java.")){
            return;
        }
        if(true){
            //利用反射获取一个类的所有方法
            try{
                Class<?> clazz = classLoader.loadClass(className);
                //这里获取类的所有方法，但是无法获取父类的方法，不过这里没必要关系父类的方法
                //如果要关心，那么需要调用getMethods方法即可
                Method[] allMethods = clazz.getDeclaredMethods();
                for(Method method : allMethods){
                    Class<?>[] paramTypes = method.getParameterTypes();
                    String methodName = method.getName();
                    Object[] param = new Object[paramTypes.length+1];
                    for(int i=0;i<paramTypes.length;i++){
                        param[i] = paramTypes[i];
                    }
                    String signStr = getMethodSign(method);
                    //Log.e("fuck","filter method----->:"+signStr);
                    if(TextUtils.isEmpty(signStr) || isFilterMethod(signStr)){
                        continue;
                    }

                    //开始构造Hook的方法信息
                    param[paramTypes.length] = new XC_MethodHook(){
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String methodSign = getMethodSign(param);
                            if(!TextUtils.isEmpty(methodSign) && !callMethodSignSet.contains(methodSign)){
                                //这里因为会打印日志，所以会出现app的ANR情况
                                Log.i("fuck", "call-->"+methodSign);
                                //这里还可以把方法的参数值打印出来，不过如果应用过大，这里会出现ANR
                                for(int i=0;i<param.args.length;i++){
                                    Log.i("fuck", "==>arg"+i+":"+param.args[i]);
                                }
                                Log.i("fuck","method result----->:"+param.getResult());
                                callMethodSignSet.add(methodSign);
                            }
                            super.afterHookedMethod(param);
                        }
                    };

                    //开始进行Hook操作，注意这里有一个问题，如果一个Hook的方法数过多，会出现OOM的错误，这个是Xposed工具的问题
                    if(!TextUtils.isEmpty(signStr) && !methodSignSet.contains(signStr)){
                        //这里因为会打印日志，所以会出现app的ANR情况
                        //Log.i("jw", "all-->"+signStr);
                        methodSignSet.add(signStr);
                        XposedHelpers.findAndHookMethod(className, classLoader, methodName, param);
                    }
                }
            }catch(Exception e){
            }

        }


    }

    private static String getMethodSign(XC_MethodHook.MethodHookParam param){
        try{
            StringBuilder methodSign = new StringBuilder();
            methodSign.append(Modifier.toString(param.method.getModifiers())+" ");
            Object result = param.getResult();
            if(result == null){
                methodSign.append("void ");
            }else{
                methodSign.append(result.getClass().getCanonicalName() + " ");
            }
            methodSign.append(param.method.getDeclaringClass().getCanonicalName()+"."+param.method.getName()+"(");
            for(int i=0;i<param.args.length;i++){
                //这里有一个问题：如果方法的参数值为null,那么这里就会报错! 得想个办法如何获取到参数类型？
                if(param.args[i] == null){
                    methodSign.append("?");
                }else{
                    methodSign.append(param.args[i].getClass().getCanonicalName());
                }
                if(i<param.args.length-1){
                    methodSign.append(",");
                }
            }
            methodSign.append(")");
            return methodSign.toString();
        }catch(Exception e){
            return null;
        }
    }

    private static String getMethodSign(Method method){
        try{
            //如果这个方法是继承父类的方法，也需要做过滤
            String methodClass = method.getDeclaringClass().getCanonicalName();
            if(methodClass.startsWith("android.") || methodClass.startsWith("java.")){
                return null;
            }

            StringBuilder methodSign = new StringBuilder();
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnTypes = method.getReturnType();
            methodSign.append(Modifier.toString(method.getModifiers()) + " ");
            methodSign.append(returnTypes.getCanonicalName() + " ");
            methodSign.append(methodClass+"."+method.getName()+"(");
            for(int i=0;i<paramTypes.length;i++){
                methodSign.append(paramTypes[i].getCanonicalName());
                if(i<paramTypes.length-1){
                    methodSign.append(",");
                }
            }
            methodSign.append(")");

            return methodSign.toString();



        }catch(Exception e){
            return null;
        }
    }
//没啥用 过滤的太少了
    private static boolean isFilterMethod(String methodSign){
        if("public final void java.lang.Object.wait()".equals(methodSign)){
            return true;
        }
        if("public final void java.lang.Object.wait(long,int)".equals(methodSign)){
            return true;
        }
        if("public final native java.lang.Object.wait(long)".equals(methodSign)){
            return true;
        }
        if("public boolean java.lang.Object.equals(java.lang.Object)".equals(methodSign)){
            return true;
        }
        if("public java.lang.String java.lang.Object.toString()".equals(methodSign)){
            return true;
        }
        if("public native int java.lang.Object.hashCode()".equals(methodSign)){
            return true;
        }
        if("public final native java.lang.Class java.lang.Object.getClass()".equals(methodSign)){
            return true;
        }
        if("public final native void java.lang.Object.notify()".equals(methodSign)){
            return true;
        }
        if("public final native void java.lang.Object.notifyAll()".equals(methodSign)){
            return true;
        }
        return false;
    }

    public static void hooker(ClassLoader classLoader){
        String[] classnames=GetAllClass.getAllClass(classLoader);
        for(String classname:classnames){
            if(classname.trim().startsWith("jd.wjlogin_sdk.common.WJLoginHelper.")){
                Log.e("fuck","find class success");
                hookClassInfo(classname,classLoader);
            }else{
                //Log.e("fuck1",classname);
            }

        }
    }




}

package org.marker.mushroom.ext.plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ch.qos.logback.core.util.FileUtil;
import freemarker.cache.StringTemplateLoader;
import org.marker.mushroom.freemarker.config.WebFreeMarkerConfigurer;
import org.marker.mushroom.holder.SpringContextHolder;
import org.marker.mushroom.holder.WebRealPathHolder;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.marker.mushroom.utils.FileUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;


/**
 * Pluginlet代理，主要是为了节约每次获取Method的时间，这样处理，更加高效。
 * @author marker
 * @version 1.0
 */
public class ProxyPluginlet {
	// 编码集(默认UTF-8)
	public static final String encoding = "utf-8";
	
	
	// 本地语言(默认汉语)
	public static final Locale locale = Locale.CHINA;
	
	
	/** freeMarker模版引擎配置 */
	private Configuration cfg;
	
	
	// 实例
	private Pluginlet object;
	
	// Method - URI
	private Map<String, Method>  getUrlMapping = new HashMap<String, Method>();
	private Map<String, Method> postUrlMapping = new HashMap<String, Method>();
	
	
	
	
    /** 模板加载路径 */
	private String templateFilePath;



    /**   */
    private StringTemplateLoader loader;
	
	/**
	 * 初始化
	 * @param pluginlet
	 * @throws Exception 
	 */
	public ProxyPluginlet(Pluginlet pluginlet ) throws Exception {
		this.object = pluginlet; 
		this.init();

        WebFreeMarkerConfigurer freeMarkerConfigurer = SpringContextHolder.getBean("webFrontConfiguration");
        loader = SpringContextHolder.getBean("stringTemplateLoader");

        cfg = freeMarkerConfigurer.getConfiguration();


        templateFilePath = WebRealPathHolder.REAL_PATH + "modules" + File.separator + this.object._config.get("module")+File.separator;


    }

	






	/**
	 * 初始化方法
	 * @throws Exception 
	 */
	private void init() throws Exception{
		Map<String,String> routers = this.object.routers;
		for(String key : routers.keySet()){
			String[] uri = key.split(":");
			String methodName = routers.get(key);
			if(1 == uri.length){ // get
				Method me = this.object.getClass().getDeclaredMethod(methodName);
				getUrlMapping.put(uri[0], me);
			}else{
				if("post".equals(uri[0].trim().toLowerCase())){// post
					Method me = this.object.getClass().getDeclaredMethod(methodName);
					postUrlMapping.put(uri[1], me);
				}else{ // get
					Method me = this.object.getClass().getDeclaredMethod(methodName);
					getUrlMapping.put(uri[1], me);
				};	
			}
		}
	}
	
	
	
 

	/**
	 * 根据URL调用对应的Pluginlet方法 
	 * @param url
	 * @throws Exception  
	 */
	public ViewObject invoke(String httpMethod, String url) throws Exception{ 
		Map<String,Method> urlMaping;
		if("GET".equals(httpMethod))
			urlMaping = this.getUrlMapping;
		else 
			urlMaping = this.postUrlMapping;
		
		Method me = urlMaping.get(url);
		if(me != null){
			Object viewHTML = me.invoke(object);
			if(null == viewHTML){
				return null;
			}
			ViewObject view = new ViewObject();
			view.setType(ViewType.HTML);
			
			view.setResult(viewHTML);
			return view;
		}
		return null;
	}
 

	public Map<String,Object> getConfig(){
		return object._config;
	}
	
	public String getModuleUUID(){
		return (String) object._config.get("module");
	}


    /**
     *
     * @param path
     * @return
     * @throws IOException
     */
	public Template getTemplate(String path) throws IOException {

        Template template = null;
        try {
            template = cfg.getTemplate(path);
        } catch (IOException e) {

            if(null == template){
                String content = com.mchange.io.FileUtils.getContentsAsString(new File(templateFilePath + path));


                loader.putTemplate(path, content);
            }
        }


        return cfg.getTemplate(path);
	}

	public ViewObject invoke(String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method method =  this.object.getClass().getDeclaredMethod(methodName);
		String viewHtml = (String) method.invoke(this.object);
        ViewObject view = new ViewObject();
        view.setType(ViewType.HTML);

        view.setResult(viewHtml);
        return view;
	}
}

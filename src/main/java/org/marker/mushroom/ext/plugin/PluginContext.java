package org.marker.mushroom.ext.plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
 


import org.marker.mushroom.context.ActionContext;
import org.marker.mushroom.ext.plugin.freemarker.EmbedDirectiveInvokeTag;
import org.marker.mushroom.freemarker.LoadDirective;
import org.marker.mushroom.freemarker.UpperDirective;
import org.marker.urlrewrite.freemarker.FrontURLRewriteMethodModel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import freemarker.template.Template;


/**
 * 插件容器，主要存放插件
 * @author marker
 * @version 1.0
 */
public class PluginContext {
	
	

	
	
	// 存放HTTP插件
	// 插件路径/代理 ，主要是因为这块的热部署功能，因此使用并发库中线程安全HanMap。
	private static final Map<String, ProxyPluginlet> pluginLets = new ConcurrentHashMap<String, ProxyPluginlet>();
	
	
	
	
	private PluginContext(){ }
	
	/**
	 * 这种写法最大的美在于，完全使用了Java虚拟机的机制进行同步保证。
	 * */
	private static class SingletonHolder {
		public final static PluginContext instance = new PluginContext();     
	}
	
	
	/**
	 * 获取数据库配置实例
	 * */
	public static PluginContext getInstance(){
		return SingletonHolder.instance;
	}
	

	
	 
	
	/**
	 * 添加分发器
	 * @param url
	 * @param action
	 * @throws Exception 
	 */
	public void put(Pluginlet pluginlet) throws Exception{
		
		String type = pluginlet._config.get(Pluginlet.CONFIG_TYPE).toString();
		
		pluginLets.put(type, new ProxyPluginlet(pluginlet));
		
		// 数据库持久化
		
	}
	
	
	/**
	 * 移除分发器
	 * @param clzz 类
	 * @throws Exception 
	 */
	public void remove(String type) throws Exception{ 
		pluginLets.remove(type); 
		// 数据库同步
	}


	/**
	 * Pluginlet 调用
	 * @param httpMethod 请求方法
	 * @param uri URI
	 * @return
	 * @throws Exception
	 */                      // POST              /guestbook/add
	public ViewObject invoke(String httpMethod, String uri) throws Exception { 
		if(httpMethod == null)
			throw new Exception("request method invalid");
		int index = uri.indexOf("/");
		if(index != -1){// 解析成功
			String pluginName = uri.substring(0, index);// 插件名称
			String pluginCurl = uri.substring(index, uri.length());// 插件功能URL 
			ProxyPluginlet pluginProxy = pluginLets.get(pluginName);
			if(pluginProxy != null){
				ViewObject view = pluginProxy.invoke(httpMethod, pluginCurl); 
					if(view != null){// 如果返回值为空，代表是自己手动处理
					Writer out = ActionContext.getResp().getWriter();
					switch(view.getType()){
					case JSON :
						JSON.writeJSONStringTo(view.getResult(), out, SerializerFeature.WriteClassName);
					     
						;break;
					case HTML:
						String path = "views"+File.separator+ view.getResult();
						
						Template template = pluginProxy.getTemplate(path);
						
						ServletContext application = ActionContext.getApplication();
						HttpServletRequest request = ActionContext.getReq();
						
						Map<String,Object> root = new HashMap<String,Object>(); 

						template.process(root, out);
					
						break;
					default:
						break;
					} 
					out.flush();
					out.close();
				} 
			}
		}
		return null;
	}




	/**
	 * Pluginlet 调用
	 * @param pluginName
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public String invokeMethod(String pluginName, String method) throws Exception {
        ProxyPluginlet pluginProxy = pluginLets.get(pluginName);
        if(pluginProxy != null){
            ViewObject view = pluginProxy.invoke(method);
            if(view != null){// 如果返回值为空，代表是自己手动处理
                ByteArrayOutputStream baos  = new ByteArrayOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(baos);
                BufferedWriter out = new BufferedWriter(outputStreamWriter);


                switch(view.getType()){
                    case HTML:
                        String path = "views" + File.separator+ view.getResult();

                        Template template = pluginProxy.getTemplate(path);

                        Map<String,Object> root = new HashMap<>();

                        template.process(root, out);

                        return new String(baos.toByteArray());

                    default:
                        break;
                }
                out.flush();
                out.close();
			}
		}
		return null;
	}








	/**
	 * 获取当前维护的Pluginlet代理
	 * @return
	 */
	public Map<String, ProxyPluginlet> getPluginLet(){ 
		return pluginLets;
	}


	public List<Object> getList() {
		List<Object> list = new ArrayList<Object>();
		Set<String> sets = pluginLets.keySet();
		Iterator<String> it =sets.iterator(); 
		while(it.hasNext()){ 
			ProxyPluginlet let = pluginLets.get(it.next()); 
			list.add(let.getConfig());
		}  
		return list;
	}


 
}
